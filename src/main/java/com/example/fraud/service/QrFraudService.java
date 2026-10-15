package com.example.fraud.service;

import com.example.fraud.model.QrScan;
import com.example.fraud.repository.QrScanRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class QrFraudService {

    private final QrScanRepository repository;

    public QrFraudService(QrScanRepository repository) {
        this.repository = repository;
    }

    // Verified UPI handles used by real banks and apps
    private static final String[] KNOWN_HANDLES = {
            "oksbi", "okhdfcbank", "okicici", "okaxis", "okbizaxis",
            "ybl", "ibl", "axl", "paytm", "apl", "upi", "abfspay",
            "sbi", "hdfcbank", "icici", "axisbank", "kotak", "yesbank",
            "pnb", "barodampay", "idfcbank", "indus", "fbl", "jupiteraxis",
            "airtel", "freecharge", "timecosmos", "waaxis", "rapl", "naviaxis"
    };

    // Words scammers put inside the UPI ID to look official
    private static final String[] SUSPICIOUS_VPA_WORDS = {
            "refund", "cashback", "prize", "lottery", "winner", "reward",
            "verify", "verification", "kyc", "support", "helpline",
            "customercare", "custcare", "officer", "police", "army",
            "olx", "quikr", "helpdesk", "claim", "bonus", "lucky"
    };

    // Bait words in the transaction note
    private static final String[] BAIT_NOTE_WORDS = {
            "refund", "cashback", "prize", "lottery", "winner", "reward",
            "receive", "credit", "claim", "bonus", "gift", "free",
            "get money", "money back", "return amount"
    };

    private static final String[] SHORTENERS = {
            "bit.ly", "tinyurl", "goo.gl", "t.co", "ow.ly", "is.gd",
            "cutt.ly", "rebrand.ly", "shorturl", "rb.gy"
    };

    // ---------------- MAIN SCAN ----------------

    public QrScan scan(QrScan qr) {

        String raw = safe(qr.getRawData()).trim();
        String low = raw.toLowerCase();

        int score = 0;
        List<String> reasons = new ArrayList<>();

        String qrType;
        String vpa = "";
        String payeeName = "";
        String amount = "";

        if (low.startsWith("upi://") || low.startsWith("upi:")) {

            Map<String, String> params = parseParams(raw);

            vpa = safe(params.get("pa"));
            payeeName = safe(params.get("pn"));
            amount = safe(params.get("am"));

            String note = safe(params.get("tn")).toLowerCase();
            String currency = safe(params.get("cu"));
            String merchantCode = safe(params.get("mc"));
            String mode = safe(params.get("mode"));

            boolean isCollect = low.startsWith("upi://collect")
                    || low.contains("purpose=collect")
                    || "02".equals(mode);

            qrType = isCollect ? "UPI COLLECT REQUEST" : "UPI PAYMENT QR";

            // RULE 1: Collect request - money leaves YOUR account
            if (isCollect) {
                score += 40;
                reasons.add("This is a COLLECT request - money will be DEBITED from you, not credited [+40]");
            }

            // RULE 2: Suspicious word inside the UPI ID
            String badWord = findFirst(vpa.toLowerCase(), SUSPICIOUS_VPA_WORDS);
            if (badWord != null) {
                score += 30;
                reasons.add("UPI ID contains a scam keyword (\"" + badWord + "\") [+30]");
            }

            // RULE 3: Unknown bank handle
            if (vpa.contains("@")) {
                String handle = vpa.substring(vpa.indexOf("@") + 1).toLowerCase();
                boolean known = false;
                for (String h : KNOWN_HANDLES) {
                    if (handle.equals(h)) {
                        known = true;
                        break;
                    }
                }
                if (!known) {
                    score += 25;
                    reasons.add("Unrecognised UPI handle \"@" + handle + "\" - not a verified bank/PSP [+25]");
                }
            } else if (!vpa.isEmpty()) {
                score += 25;
                reasons.add("Malformed UPI ID - missing @ symbol [+25]");
            }

            // RULE 4: More than one @ = spoofing attempt
            if (countChar(vpa, '@') > 1) {
                score += 25;
                reasons.add("UPI ID contains multiple @ symbols (spoofing attempt) [+25]");
            }

            // RULE 5: Bait words in the note
            String bait = findFirst(note, BAIT_NOTE_WORDS);
            if (bait != null) {
                score += 30;
                reasons.add("Note promises money (\"" + bait + "\") - QR codes can only SEND money [+30]");
            }

            // RULE 6: Amount already filled in
            if (!amount.isEmpty()) {
                try {
                    double amt = Double.parseDouble(amount);
                    if (amt > 10000) {
                        score += 20;
                        reasons.add("Large amount pre-filled (Rs " + amount + ") [+20]");
                    } else if (amt > 5000) {
                        score += 10;
                        reasons.add("Amount pre-filled (Rs " + amount + ") - verify before paying [+10]");
                    }
                } catch (Exception ex) {
                    score += 15;
                    reasons.add("Amount field is not a valid number [+15]");
                }
            }

            // RULE 7: No payee name
            if (payeeName.isEmpty()) {
                score += 15;
                reasons.add("No payee name in the QR - genuine merchant QRs always show a name [+15]");
            }

            // RULE 8: Merchant QR with no merchant code
            if (!merchantCode.isEmpty() && merchantCode.length() < 4) {
                score += 10;
                reasons.add("Invalid merchant category code [+10]");
            }

            // RULE 9: Currency other than INR
            if (!currency.isEmpty() && !currency.equalsIgnoreCase("INR")) {
                score += 10;
                reasons.add("Currency is " + currency + " instead of INR [+10]");
            }

            // RULE 10: Hidden redirect parameter
            if (low.contains("url=") || low.contains("&link=")) {
                score += 25;
                reasons.add("QR contains a hidden redirect link [+25]");
            }

        } else if (low.startsWith("http://") || low.startsWith("https://")) {

            qrType = "WEBSITE LINK";

            score += 40;
            reasons.add("This is NOT a payment QR - it opens a website instead [+40]");

            String sh = findFirst(low, SHORTENERS);
            if (sh != null) {
                score += 30;
                reasons.add("Destination hidden behind a shortener (" + sh + ") [+30]");
            }

            if (Pattern.compile("https?://\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}").matcher(low).find()) {
                score += 35;
                reasons.add("Link points to a raw IP address instead of a domain [+35]");
            }

            if (low.startsWith("http://")) {
                score += 15;
                reasons.add("Insecure HTTP link (not HTTPS) [+15]");
            }

            String bad = findFirst(low, SUSPICIOUS_VPA_WORDS);
            if (bad != null) {
                score += 20;
                reasons.add("Link text contains a scam keyword (\"" + bad + "\") [+20]");
            }

            if (low.contains(".apk") || low.contains("download")) {
                score += 30;
                reasons.add("Link tries to download an app or file [+30]");
            }

        } else if (low.startsWith("tel:") || low.startsWith("sms:")) {

            qrType = "PHONE / SMS";
            score += 45;
            reasons.add("QR triggers a call or SMS - common in remote-access scams [+45]");

        } else {

            qrType = "UNKNOWN FORMAT";
            score += 35;
            reasons.add("Unrecognised QR format - not a standard UPI payment code [+35]");
        }

        // RULE 11: Obfuscated payload
        if (raw.length() > 300) {
            score += 10;
            reasons.add("Unusually long QR payload - possible obfuscation [+10]");
        }

        if (score > 100) {
            score = 100;
        }

        if (reasons.isEmpty()) {
            reasons.add("Standard UPI payment QR with a verified handle - no red flags found");
        }

        // ---------------- VERDICT ----------------
        String verdict;
        String advice;

        if (score < 20) {
            verdict = "SAFE";
            advice = "Looks like a genuine payment QR. Always confirm the payee name on your UPI app before you pay.";
        } else if (score < 40) {
            verdict = "LOW RISK";
            advice = "A few unusual details. Verify the merchant name and amount carefully before paying.";
        } else if (score < 70) {
            verdict = "SUSPICIOUS";
            advice = "Likely a scam QR. DO NOT scan this in your UPI app. Verify the receiver by phone first.";
        } else {
            verdict = "DANGEROUS";
            advice = "DO NOT SCAN. DO NOT PAY. DO NOT ENTER YOUR UPI PIN. Report this QR and delete it.";
        }

        qr.setPayeeVpa(vpa);
        qr.setPayeeName(payeeName);
        qr.setAmount(amount);
        qr.setQrType(qrType);
        qr.setRiskScore(score);
        qr.setVerdict(verdict);
        qr.setScannedAt(LocalDateTime.now());

        QrScan saved = repository.save(qr);
        saved.setReasons(reasons);
        saved.setAdvice(advice);

        return saved;
    }

    // ---------------- HELPERS ----------------

    private Map<String, String> parseParams(String raw) {

        Map<String, String> map = new HashMap<>();

        int q = raw.indexOf("?");
        if (q < 0 || q == raw.length() - 1) {
            return map;
        }

        String query = raw.substring(q + 1);
        String[] pairs = query.split("&");

        for (String pair : pairs) {
            int eq = pair.indexOf("=");
            if (eq > 0) {
                String key = pair.substring(0, eq).trim().toLowerCase();
                String val = pair.substring(eq + 1).trim();
                val = val.replace("%20", " ").replace("+", " ");
                map.put(key, val);
            }
        }

        return map;
    }

    private String safe(String s) {
        return (s == null) ? "" : s;
    }

    private String findFirst(String text, String[] words) {
        for (String w : words) {
            if (text.contains(w)) {
                return w;
            }
        }
        return null;
    }

    private int countChar(String s, char c) {
        int n = 0;
        for (char ch : s.toCharArray()) {
            if (ch == c) {
                n++;
            }
        }
        return n;
    }

    // ---------------- HISTORY ----------------

    public List<QrScan> getAll() {
        return repository.findAll();
    }

    public Map<String, Object> getStats() {

        List<QrScan> all = repository.findAll();

        long safeCount = 0;
        long low = 0;
        long sus = 0;
        long danger = 0;

        for (QrScan q : all) {
            String v = q.getVerdict();
            if ("SAFE".equals(v)) {
                safeCount++;
            } else if ("LOW RISK".equals(v)) {
                low++;
            } else if ("SUSPICIOUS".equals(v)) {
                sus++;
            } else if ("DANGEROUS".equals(v)) {
                danger++;
            }
        }

        Map<String, Object> m = new HashMap<>();
        m.put("total", all.size());
        m.put("safe", safeCount);
        m.put("lowRisk", low);
        m.put("suspicious", sus);
        m.put("dangerous", danger);
        m.put("threats", sus + danger);

        return m;
    }

    public void deleteAll() {
        repository.deleteAll();
    }
}