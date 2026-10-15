package com.example.fraud.service;

import com.example.fraud.model.EmailScan;
import com.example.fraud.repository.EmailScanRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PhishingDetectionService {

    private final EmailScanRepository repository;

    public PhishingDetectionService(EmailScanRepository repository) {
        this.repository = repository;
    }

    // ---------- KEYWORD LISTS ----------

    private static final String[] OTP_WORDS = {
            "otp", "one time password", "one-time password", "cvv", "atm pin",
            "card pin", "share your pin", "verification code", "security code"
    };

    private static final String[] CREDENTIAL_WORDS = {
            "verify your account", "confirm your password", "update your kyc",
            "re-activate", "reactivate your account", "login to continue",
            "confirm your identity", "validate your account", "update your details",
            "click here to login", "sign in to verify"
    };

    private static final String[] PAYMENT_WORDS = {
            "transfer the amount", "make a payment", "pay immediately", "wire transfer",
            "send money", "processing fee", "clearance fee", "customs fee",
            "release fee", "advance payment", "pay now to avoid", "outstanding payment"
    };

    private static final String[] CRYPTO_WORDS = {
            "bitcoin", "btc wallet", "usdt", "crypto wallet", "gift card",
            "google play card", "amazon gift card", "itunes card", "paytm to this number"
    };

    private static final String[] PRIZE_WORDS = {
            "you have won", "you won", "lottery", "lucky winner", "claim your prize",
            "inheritance", "unclaimed funds", "beneficiary", "cash prize",
            "congratulations you", "selected winner", "jackpot"
    };

    private static final String[] URGENCY_WORDS = {
            "urgent", "immediately", "act now", "within 24 hours", "expires today",
            "last warning", "final notice", "limited time", "hurry", "asap"
    };

    private static final String[] THREAT_WORDS = {
            "account will be closed", "account suspended", "account blocked",
            "legal action", "will be arrested", "penalty", "your account is locked",
            "service will be terminated", "fir will be filed", "court notice"
    };

    private static final String[] SHORTENERS = {
            "bit.ly", "tinyurl", "goo.gl", "t.co", "ow.ly", "is.gd",
            "buff.ly", "rebrand.ly", "cutt.ly", "shorturl"
    };

    private static final String[] FREE_MAIL = {
            "gmail.com", "yahoo.com", "hotmail.com", "outlook.com",
            "rediffmail.com", "protonmail.com", "mail.ru"
    };

    private static final String[] INSTITUTION_WORDS = {
            "bank", "sbi", "hdfc", "icici", "axis", "paypal", "paytm",
            "income tax", "government", "rbi", "customs", "police", "amazon",
            "netflix", "microsoft", "apple support"
    };

    private static final String[] ATTACHMENT_RISK = {
            ".exe", ".scr", ".bat", ".vbs", ".jar", ".apk", ".zip attached",
            "invoice.zip", "document.exe"
    };

    // ---------- MAIN SCAN ----------

    public EmailScan scan(EmailScan email) {

        String sender = safe(email.getSender()).toLowerCase();
        String subject = safe(email.getSubject()).toLowerCase();
        String body = safe(email.getContent());
        String text = (subject + " " + body).toLowerCase();

        int score = 0;
        List<String> reasons = new ArrayList<>();

        // RULE 1: Asking for OTP / PIN / CVV  (CRITICAL)
        String hit = findFirst(text, OTP_WORDS);
        if (hit != null) {
            score += 40;
            reasons.add("Requests OTP / PIN / CVV (\"" + hit + "\") - banks NEVER ask this [+40]");
        }

        // RULE 2: Credential harvesting
        hit = findFirst(text, CREDENTIAL_WORDS);
        if (hit != null) {
            score += 30;
            reasons.add("Credential harvesting phrase (\"" + hit + "\") [+30]");
        }

        // RULE 3: Demands payment
        hit = findFirst(text, PAYMENT_WORDS);
        if (hit != null) {
            score += 30;
            reasons.add("Demands a payment (\"" + hit + "\") [+30]");
        }

        // RULE 4: Untraceable payment method
        hit = findFirst(text, CRYPTO_WORDS);
        if (hit != null) {
            score += 35;
            reasons.add("Asks for untraceable payment (\"" + hit + "\") [+35]");
        }

        // RULE 5: Prize / lottery scam
        hit = findFirst(text, PRIZE_WORDS);
        if (hit != null) {
            score += 35;
            reasons.add("Classic prize / lottery scam (\"" + hit + "\") [+35]");
        }

        // RULE 6: Urgency pressure
        hit = findFirst(text, URGENCY_WORDS);
        if (hit != null) {
            score += 20;
            reasons.add("Creates false urgency (\"" + hit + "\") [+20]");
        }

        // RULE 7: Threats
        hit = findFirst(text, THREAT_WORDS);
        if (hit != null) {
            score += 20;
            reasons.add("Threatens the user (\"" + hit + "\") [+20]");
        }

        // RULE 8: Link analysis
        List<String> links = extractLinks(body);

        for (String link : links) {
            String low = link.toLowerCase();

            String sh = findFirst(low, SHORTENERS);
            if (sh != null) {
                score += 25;
                reasons.add("Hidden shortened link (" + sh + ") [+25]");
                break;
            }
        }

        for (String link : links) {
            if (link.toLowerCase().startsWith("http://")) {
                score += 15;
                reasons.add("Insecure HTTP link (not HTTPS) [+15]");
                break;
            }
        }

        for (String link : links) {
            if (Pattern.compile("https?://\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}").matcher(link).find()) {
                score += 30;
                reasons.add("Link points to a raw IP address instead of a domain [+30]");
                break;
            }
        }

        // RULE 9: Sender impersonation
        if (!sender.isEmpty()) {
            boolean freeMail = contains(sender, FREE_MAIL);
            boolean claimsInstitution = contains(text, INSTITUTION_WORDS) || contains(sender, INSTITUTION_WORDS);

            if (freeMail && claimsInstitution) {
                score += 25;
                reasons.add("Claims to be an official institution but uses a free email account [+25]");
            }

            if (sender.contains("-") && claimsInstitution && !freeMail) {
                score += 10;
                reasons.add("Sender domain looks like a look-alike / spoofed domain [+10]");
            }
        }

        // RULE 10: Dangerous attachment mentioned
        hit = findFirst(text, ATTACHMENT_RISK);
        if (hit != null) {
            score += 25;
            reasons.add("Mentions a dangerous attachment type (" + hit + ") [+25]");
        }

        // RULE 11: Generic greeting
        if (text.contains("dear customer") || text.contains("dear user")
                || text.contains("dear sir/madam") || text.contains("dear account holder")) {
            score += 10;
            reasons.add("Generic greeting instead of your real name [+10]");
        }

        // RULE 12: Shouting in subject
        if (subject.length() > 8) {
            String rawSubject = safe(email.getSubject());
            int caps = 0;
            int letters = 0;
            for (char c : rawSubject.toCharArray()) {
                if (Character.isLetter(c)) {
                    letters++;
                    if (Character.isUpperCase(c)) {
                        caps++;
                    }
                }
            }
            if (letters > 0 && (caps * 100 / letters) > 70) {
                score += 10;
                reasons.add("Subject written in ALL CAPS (pressure tactic) [+10]");
            }
        }

        if (score > 100) {
            score = 100;
        }

        if (reasons.isEmpty()) {
            reasons.add("No known phishing pattern detected");
        }

        // ---------- VERDICT ----------
        String verdict;
        String advice;

        if (score < 20) {
            verdict = "SAFE";
            advice = "No scam indicators found. Still verify the sender before sharing anything.";
        } else if (score < 40) {
            verdict = "LOW RISK";
            advice = "A few unusual patterns. Do not click links until you confirm the sender.";
        } else if (score < 70) {
            verdict = "SUSPICIOUS";
            advice = "Likely a scam. DO NOT click any link and DO NOT share personal details.";
        } else {
            verdict = "DANGEROUS";
            advice = "DO NOT PAY. DO NOT CLICK. DO NOT SHARE OTP. Delete this email and report it.";
        }

        email.setRiskScore(score);
        email.setVerdict(verdict);
        email.setScannedAt(LocalDateTime.now());

        EmailScan saved = repository.save(email);
        saved.setReasons(reasons);
        saved.setLinks(links);
        saved.setAdvice(advice);

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

    // ---------- HISTORY & STATS ----------

    public List<EmailScan> getAll() {
        return repository.findAll();
    }

    public Map<String, Object> getStats() {

        List<EmailScan> all = repository.findAll();

        long safeCount = 0;
        long low = 0;
        long sus = 0;
        long danger = 0;

        for (EmailScan e : all) {
            String v = e.getVerdict();
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