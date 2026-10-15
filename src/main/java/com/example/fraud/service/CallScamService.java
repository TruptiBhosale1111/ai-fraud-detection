package com.example.fraud.service;

import com.example.fraud.model.CallScan;
import com.example.fraud.repository.CallScanRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CallScamService {

    private final CallScanRepository repository;

    public CallScamService(CallScanRepository repository) {
        this.repository = repository;
    }

    // ---------- SCAM PATTERN LISTS ----------

    private static final String[] KYC_WORDS = {
            "kyc", "re-kyc", "rekyc", "update your kyc", "kyc expired",
            "kyc pending", "pan card update", "aadhaar link", "aadhar link",
            "account will be blocked", "account will be suspended",
            "your sim will be blocked", "sim card will be deactivated"
    };

    private static final String[] ARREST_WORDS = {
            "digital arrest", "cbi", "narcotics", "customs department",
            "your parcel", "parcel contains", "illegal item", "money laundering",
            "fir has been registered", "arrest warrant", "court summon",
            "cyber crime branch", "enforcement directorate", "police case",
            "do not disconnect the call", "video call for verification"
    };

    private static final String[] LOTTERY_WORDS = {
            "you have won", "you won", "lottery", "lucky draw", "kbc",
            "kaun banega", "jackpot", "lucky winner", "claim your prize",
            "cash prize", "bumper prize", "selected winner", "lucky customer"
    };

    private static final String[] ELECTRICITY_WORDS = {
            "electricity bill", "power will be disconnected", "electricity connection",
            "meter will be disconnected", "bill not updated", "disconnected tonight"
    };

    private static final String[] ARMY_OLX_WORDS = {
            "army officer", "indian army", "posted at", "military camp",
            "crpf", "bsf", "i am transferred", "urgent posting",
            "olx", "quikr", "advance payment", "canteen"
    };

    private static final String[] REMOTE_APPS = {
            "anydesk", "teamviewer", "quick support", "quicksupport",
            "screen share", "airdroid", "rustdesk", "remote access",
            "install this app", "download this app"
    };

    private static final String[] OTP_WORDS = {
            "share otp", "tell me the otp", "otp received", "read me the otp",
            "share the code", "cvv", "atm pin", "card pin", "upi pin",
            "share your pin", "confirm the code"
    };

    private static final String[] JOB_LOAN_WORDS = {
            "work from home", "part time job", "earn daily", "earn per day",
            "instant loan", "loan approved", "pre-approved loan",
            "registration fee", "processing fee", "security deposit",
            "refundable deposit", "task based", "telegram job"
    };

    private static final String[] URGENCY_WORDS = {
            "urgent", "immediately", "within 24 hours", "last warning",
            "final notice", "today only", "expires today", "act now",
            "before midnight", "last chance"
    };

    private static final String[] BANK_WORDS = {
            "bank", "sbi", "hdfc", "icici", "axis", "kotak", "pnb",
            "rbi", "paytm", "phonepe", "google pay", "gpay",
            "net banking", "debit card", "credit card", "account holder"
    };

    private static final String[] SHORTENERS = {
            "bit.ly", "tinyurl", "goo.gl", "t.co", "ow.ly", "is.gd",
            "cutt.ly", "rb.gy", "shorturl", "tiny.cc"
    };

    // Genuine Indian SMS sender ID pattern: AD-HDFCBK, VM-SBIINB, JD-ICICIB
    private static final Pattern GENUINE_HEADER =
            Pattern.compile("^[A-Z]{2}-[A-Z0-9]{6}$");

    private static final Pattern INDIAN_MOBILE =
            Pattern.compile("^(\\+?91)?[6-9]\\d{9}$");

    // ---------- MAIN SCAN ----------

    public CallScan scan(CallScan c) {

        String sender = safe(c.getSender()).trim();
        String senderClean = sender.replaceAll("[\\s\\-()]", "");
        String msg = safe(c.getMessage());
        String low = msg.toLowerCase();
        String channel = safe(c.getChannel()).isEmpty() ? "SMS" : c.getChannel().toUpperCase();

        int score = 0;
        List<String> reasons = new ArrayList<>();
        String scamType = "UNKNOWN";
        String senderType = "UNKNOWN";

        // ---------- SENDER ANALYSIS ----------

        boolean claimsBank = contains(low, BANK_WORDS) || contains(sender.toLowerCase(), BANK_WORDS);

        if (!sender.isEmpty()) {

            if (GENUINE_HEADER.matcher(sender.toUpperCase()).matches()) {
                senderType = "REGISTERED SENDER ID";

            } else if (senderClean.startsWith("+") && !senderClean.startsWith("+91")) {
                senderType = "INTERNATIONAL NUMBER";
                score += 35;
                String cc = senderClean.length() > 4 ? senderClean.substring(0, 4) : senderClean;
                reasons.add("International number (" + cc + "...) claiming to be from India [+35]");

            } else if (INDIAN_MOBILE.matcher(senderClean).matches()) {
                senderType = "PERSONAL MOBILE NUMBER";
                if (claimsBank) {
                    score += 35;
                    reasons.add("Message from a personal 10-digit mobile, not a registered bank sender ID [+35]");
                } else {
                    score += 10;
                    reasons.add("Sent from a personal mobile number [+10]");
                }

            } else if (senderClean.startsWith("140")) {
                senderType = "TELEMARKETING";
                score += 10;
                reasons.add("Registered telemarketing number (140 series) [+10]");

            } else if (senderClean.matches("\\d{4,8}")) {
                senderType = "SHORT CODE";
                score += 15;
                reasons.add("Unusual short-code sender [+15]");

            } else {
                senderType = "UNVERIFIED SENDER";
                score += 15;
                reasons.add("Sender ID does not match any registered format [+15]");
            }
        }

        // ---------- CONTENT ANALYSIS ----------

        String hit;

        // 1. Digital arrest / police scam  (most dangerous)
        hit = findFirst(low, ARREST_WORDS);
        if (hit != null) {
            score += 45;
            scamType = "DIGITAL ARREST SCAM";
            reasons.add("Impersonates police / CBI / customs (\"" + hit + "\") - no agency ever arrests over a call [+45]");
        }

        // 2. Asking for OTP / PIN
        hit = findFirst(low, OTP_WORDS);
        if (hit != null) {
            score += 40;
            if ("UNKNOWN".equals(scamType)) {
                scamType = "OTP THEFT";
            }
            reasons.add("Asks for OTP / PIN / CVV (\"" + hit + "\") - banks NEVER ask this [+40]");
        }

        // 3. Remote access app
        hit = findFirst(low, REMOTE_APPS);
        if (hit != null) {
            score += 40;
            if ("UNKNOWN".equals(scamType)) {
                scamType = "REMOTE ACCESS SCAM";
            }
            reasons.add("Asks you to install a screen-sharing app (\"" + hit + "\") - gives full phone control [+40]");
        }

        // 4. KYC scam
        hit = findFirst(low, KYC_WORDS);
        if (hit != null) {
            score += 30;
            if ("UNKNOWN".equals(scamType)) {
                scamType = "FAKE KYC SCAM";
            }
            reasons.add("Fake KYC / account-block threat (\"" + hit + "\") [+30]");
        }

        // 5. Lottery
        hit = findFirst(low, LOTTERY_WORDS);
        if (hit != null) {
            score += 35;
            if ("UNKNOWN".equals(scamType)) {
                scamType = "LOTTERY SCAM";
            }
            reasons.add("Lottery / prize bait (\"" + hit + "\") [+35]");
        }

        // 6. Electricity bill
        hit = findFirst(low, ELECTRICITY_WORDS);
        if (hit != null) {
            score += 35;
            if ("UNKNOWN".equals(scamType)) {
                scamType = "ELECTRICITY BILL SCAM";
            }
            reasons.add("Fake electricity disconnection threat (\"" + hit + "\") [+35]");
        }

        // 7. Army / OLX
        hit = findFirst(low, ARMY_OLX_WORDS);
        if (hit != null) {
            score += 30;
            if ("UNKNOWN".equals(scamType)) {
                scamType = "ARMY OFFICER / OLX SCAM";
            }
            reasons.add("Fake army officer or marketplace scam (\"" + hit + "\") [+30]");
        }

        // 8. Job / loan advance fee
        hit = findFirst(low, JOB_LOAN_WORDS);
        if (hit != null) {
            score += 30;
            if ("UNKNOWN".equals(scamType)) {
                scamType = "JOB / LOAN SCAM";
            }
            reasons.add("Job or loan offer with advance fee (\"" + hit + "\") [+30]");
        }

        // 9. Urgency pressure
        hit = findFirst(low, URGENCY_WORDS);
        if (hit != null) {
            score += 15;
            reasons.add("Creates false urgency (\"" + hit + "\") [+15]");
        }

        // ---------- LINK ANALYSIS ----------

        List<String> links = extractLinks(msg);

        if (!links.isEmpty()) {

            for (String l : links) {
                String ll = l.toLowerCase();

                String sh = findFirst(ll, SHORTENERS);
                if (sh != null) {
                    score += 25;
                    reasons.add("Hidden shortened link (" + sh + ") [+25]");
                    break;
                }
            }

            for (String l : links) {
                if (l.toLowerCase().contains(".apk")) {
                    score += 35;
                    reasons.add("Link downloads an APK file - malware risk [+35]");
                    break;
                }
            }

            for (String l : links) {
                if (Pattern.compile("https?://\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}").matcher(l).find()) {
                    score += 30;
                    reasons.add("Link points to a raw IP address [+30]");
                    break;
                }
            }

            for (String l : links) {
                if (l.toLowerCase().startsWith("http://")) {
                    score += 15;
                    reasons.add("Insecure HTTP link (not HTTPS) [+15]");
                    break;
                }
            }

            if (claimsBank) {
                score += 15;
                reasons.add("Bank-related message containing a clickable link [+15]");
            }
        }

        // 10. Asks to call a number back
        if (low.contains("call on") || low.contains("call back on")
                || low.contains("contact us at") || low.contains("call this number")) {
            score += 20;
            reasons.add("Asks you to call an unlisted helpline number [+20]");
        }

        // 11. Generic greeting
        if (low.contains("dear customer") || low.contains("dear user")
                || low.contains("dear sir/madam") || low.contains("dear consumer")) {
            score += 10;
            reasons.add("Generic greeting instead of your real name [+10]");
        }

        if (score > 100) {
            score = 100;
        }

        if (reasons.isEmpty()) {
            reasons.add("No known scam pattern detected");
        }

        if ("UNKNOWN".equals(scamType) && score >= 40) {
            scamType = "GENERIC PHISHING";
        }
        if (score < 20) {
            scamType = "NONE DETECTED";
        }

        // ---------- VERDICT ----------
        String verdict;
        String advice;

        if (score < 20) {
            verdict = "SAFE";
            advice = "No scam indicators found. Still avoid sharing OTP or personal details with anyone.";
        } else if (score < 40) {
            verdict = "LOW RISK";
            advice = "Some unusual patterns. Verify by calling the official number printed on your bank card.";
        } else if (score < 70) {
            verdict = "SUSPICIOUS";
            advice = "Likely a scam. Do NOT click any link, do NOT call back, do NOT share any details.";
        } else {
            verdict = "DANGEROUS";
            advice = "DISCONNECT THE CALL. DO NOT PAY. DO NOT SHARE OTP. Block this number and report it on cybercrime.gov.in or call 1930.";
        }

        c.setChannel(channel);
        c.setScamType(scamType);
        c.setRiskScore(score);
        c.setVerdict(verdict);
        c.setScannedAt(LocalDateTime.now());

        CallScan saved = repository.save(c);
        saved.setReasons(reasons);
        saved.setLinks(links);
        saved.setAdvice(advice);
        saved.setSenderType(senderType);

        return saved;
    }

    // ---------- HELPERS ----------

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

    private boolean contains(String text, String[] words) {
        return findFirst(text, words) != null;
    }

    private List<String> extractLinks(String body) {

        List<String> found = new ArrayList<>();

        Pattern p = Pattern.compile("(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)");
        Matcher m = p.matcher(safe(body));

        while (m.find()) {
            String link = m.group(1);
            if (!found.contains(link)) {
                found.add(link);
            }
        }

        return found;
    }

    // ---------- HISTORY ----------

    public List<CallScan> getAll() {
        return repository.findAll();
    }

    public Map<String, Object> getStats() {

        List<CallScan> all = repository.findAll();

        long safeCount = 0;
        long low = 0;
        long sus = 0;
        long danger = 0;

        Map<String, Integer> typeCount = new HashMap<>();

        for (CallScan c : all) {

            String v = c.getVerdict();
            if ("SAFE".equals(v)) {
                safeCount++;
            } else if ("LOW RISK".equals(v)) {
                low++;
            } else if ("SUSPICIOUS".equals(v)) {
                sus++;
            } else if ("DANGEROUS".equals(v)) {
                danger++;
            }

            String t = c.getScamType();
            if (t != null && !"NONE DETECTED".equals(t)) {
                typeCount.put(t, typeCount.getOrDefault(t, 0) + 1);
            }
        }

        Map<String, Object> m = new HashMap<>();
        m.put("total", all.size());
        m.put("safe", safeCount);
        m.put("lowRisk", low);
        m.put("suspicious", sus);
        m.put("dangerous", danger);
        m.put("threats", sus + danger);
        m.put("types", typeCount);

        return m;
    }

    public void deleteAll() {
        repository.deleteAll();
    }
}