package com.example.fraud.service;

import com.example.fraud.model.OfferScan;
import com.example.fraud.repository.OfferScanRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OfferScamService {

    private final OfferScanRepository repository;

    public OfferScamService(OfferScanRepository repository) {
        this.repository = repository;
    }

    // ---------- PATTERN LISTS ----------

    // The single biggest red flag - pay before you earn
    private static final String[] ADVANCE_FEE_WORDS = {
            "registration fee", "registration charge", "processing fee", "processing charge",
            "security deposit", "refundable deposit", "caution deposit", "training fee",
            "interview fee", "joining fee", "onboarding fee", "documentation charge",
            "file charge", "verification fee", "activation fee", "membership fee",
            "kit charge", "laptop deposit", "id card fee", "placement fee",
            "service charge", "consultancy fee", "admin fee", "gst charge",
            "convenience fee", "token amount", "pay to confirm", "pay first"
    };

    // Loan specific
    private static final String[] LOAN_SCAM_WORDS = {
            "instant loan", "loan approved", "pre-approved loan", "pre approved loan",
            "loan sanctioned", "guaranteed approval", "no credit check", "without cibil",
            "no cibil", "bad cibil ok", "loan without documents", "no documents required",
            "minutes disbursal", "loan in 5 minutes", "instant cash", "instant disbursal",
            "insurance charge", "nbfc approved", "rbi approved loan"
    };

    // Unrealistic earnings
    private static final String[] EARNING_BAIT = {
            "earn daily", "earn per day", "daily payout", "daily income",
            "earn from home", "work from home job", "no experience required",
            "no skills required", "anyone can apply", "housewives can apply",
            "students can apply", "earn while you learn", "unlimited income",
            "guaranteed income", "fixed income daily", "part time job"
    };

    // Task / Telegram investment scams
    private static final String[] TASK_SCAM_WORDS = {
            "telegram", "whatsapp group", "prepaid task", "merchant task",
            "like and subscribe", "youtube like", "rating task", "review task",
            "complete task", "recharge task", "combination task", "lucky task",
            "deposit to unlock", "recharge to continue", "commission task",
            "invite and earn", "referral income"
    };

    // Hiring process red flags
    private static final String[] NO_PROCESS_WORDS = {
            "selected without interview", "no interview required", "direct joining",
            "selected directly", "offer letter without interview", "instant joining",
            "your resume is shortlisted", "shortlisted from portal",
            "limited seats", "limited vacancy", "only few positions",
            "confirm within 24 hours", "offer expires today"
    };

    // Data harvesting
    private static final String[] DATA_HARVEST_WORDS = {
            "share your aadhaar", "aadhaar card photo", "pan card photo",
            "cancelled cheque", "bank passbook photo", "share bank details",
            "account number and ifsc", "share your otp", "atm pin",
            "net banking password", "debit card number", "cvv"
    };

    // Unofficial contact channels
    private static final String[] FREE_MAIL = {
            "gmail.com", "yahoo.com", "hotmail.com", "outlook.com",
            "rediffmail.com", "proton.me", "protonmail.com", "mail.ru"
    };

    // Big-name companies scammers impersonate
    private static final String[] BIG_COMPANIES = {
            "amazon", "flipkart", "tcs", "infosys", "wipro", "accenture",
            "google", "microsoft", "reliance", "tata", "hcl", "cognizant",
            "capgemini", "deloitte", "swiggy", "zomato", "byju", "paytm",
            "hdfc", "icici", "sbi", "axis", "bajaj finserv", "indiamart"
    };

    private static final String[] SHORTENERS = {
            "bit.ly", "tinyurl", "goo.gl", "t.co", "ow.ly", "is.gd",
            "cutt.ly", "rb.gy", "shorturl", "tiny.cc"
    };

    private static final String[] PAY_CHANNEL_WORDS = {
            "google pay", "gpay", "phonepe", "paytm number", "upi id",
            "scan the qr", "send to this number", "personal account",
            "bitcoin", "usdt", "crypto", "gift card"
    };

    private static final Pattern INDIAN_MOBILE =
            Pattern.compile("^(\\+?91)?[6-9]\\d{9}$");

    // ---------- MAIN SCAN ----------

    public OfferScan scan(OfferScan o) {

        String type = safe(o.getOfferType()).isEmpty() ? "JOB" : o.getOfferType().toUpperCase();
        String company = safe(o.getCompanyName());
        String contact = safe(o.getContactInfo()).trim();
        String body = safe(o.getContent());

        String low = (company + " " + contact + " " + body).toLowerCase();
        String contactLow = contact.toLowerCase();
        String contactClean = contact.replaceAll("[\\s\\-()]", "");

        int score = 0;
        List<String> reasons = new ArrayList<>();
        String scamType = "UNKNOWN";
        String contactType = "NOT PROVIDED";

        String hit;

        // ---------- RULE 1: ADVANCE FEE (the killer signal) ----------
        hit = findFirst(low, ADVANCE_FEE_WORDS);
        if (hit != null) {
            score += 45;
            scamType = "ADVANCE FEE SCAM";
            reasons.add("Demands money upfront (\"" + hit + "\") - genuine employers/lenders NEVER charge candidates [+45]");
        }

        // ---------- RULE 2: Fee amount detected ----------
        String fee = extractAmount(body);
        if (!fee.isEmpty()) {
            score += 20;
            reasons.add("A specific fee amount is mentioned (Rs " + fee + ") [+20]");
        }

        // ---------- RULE 3: Data harvesting ----------
        hit = findFirst(low, DATA_HARVEST_WORDS);
        if (hit != null) {
            score += 40;
            if ("UNKNOWN".equals(scamType)) {
                scamType = "IDENTITY THEFT";
            }
            reasons.add("Asks for sensitive documents / OTP before selection (\"" + hit + "\") [+40]");
        }

        // ---------- RULE 4: Loan-specific red flags ----------
        hit = findFirst(low, LOAN_SCAM_WORDS);
        if (hit != null) {
            score += 30;
            if ("UNKNOWN".equals(scamType)) {
                scamType = "FAKE LOAN OFFER";
            }
            reasons.add("Unrealistic loan promise (\"" + hit + "\") - no lender approves without verification [+30]");
        }

        // ---------- RULE 5: Task / Telegram scam ----------
        hit = findFirst(low, TASK_SCAM_WORDS);
        if (hit != null) {
            score += 35;
            if ("UNKNOWN".equals(scamType)) {
                scamType = "TASK / TELEGRAM SCAM";
            }
            reasons.add("Task-based or Telegram earning scheme (\"" + hit + "\") [+35]");
        }

        // ---------- RULE 6: Unrealistic earnings ----------
        hit = findFirst(low, EARNING_BAIT);
        if (hit != null) {
            score += 25;
            if ("UNKNOWN".equals(scamType)) {
                scamType = "FAKE JOB OFFER";
            }
            reasons.add("Too-good-to-be-true earning claim (\"" + hit + "\") [+25]");
        }

        // ---------- RULE 7: Salary figure sanity check ----------
        int salary = extractSalary(body);
        if (salary >= 50000 && containsAny(low, EARNING_BAIT)) {
            score += 25;
            reasons.add("Very high salary (Rs " + salary + ") offered for an unskilled work-from-home role [+25]");
        }

        // ---------- RULE 8: No hiring process ----------
        hit = findFirst(low, NO_PROCESS_WORDS);
        if (hit != null) {
            score += 25;
            if ("UNKNOWN".equals(scamType)) {
                scamType = "FAKE JOB OFFER";
            }
            reasons.add("No genuine hiring process (\"" + hit + "\") [+25]");
        }

        // ---------- RULE 9: Contact channel analysis ----------
        if (!contact.isEmpty()) {

            if (contact.contains("@") && contact.contains(".")) {

                String domain = contact.substring(contact.indexOf("@") + 1).toLowerCase();
                boolean freeMail = false;

                for (String f : FREE_MAIL) {
                    if (domain.equals(f)) {
                        freeMail = true;
                        break;
                    }
                }

                if (freeMail) {
                    contactType = "FREE EMAIL ACCOUNT";
                    boolean claimsBig = containsAny(low, BIG_COMPANIES);

                    if (claimsBig) {
                        score += 35;
                        reasons.add("Claims to be a well-known company but uses a free email account [+35]");
                    } else {
                        score += 20;
                        reasons.add("HR contact uses a free email account, not a company domain [+20]");
                    }
                } else {
                    contactType = "COMPANY DOMAIN";
                }

            } else if (INDIAN_MOBILE.matcher(contactClean).matches()) {
                contactType = "PERSONAL MOBILE";
                score += 20;
                reasons.add("Only a personal mobile number given, no official email or website [+20]");

            } else if (contactLow.contains("t.me") || contactLow.contains("telegram")) {
                contactType = "TELEGRAM";
                score += 35;
                reasons.add("Recruitment conducted over Telegram - hallmark of task scams [+35]");

            } else if (contactLow.contains("wa.me") || contactLow.contains("whatsapp")) {
                contactType = "WHATSAPP ONLY";
                score += 25;
                reasons.add("WhatsApp-only contact with no official channel [+25]");

            } else {
                contactType = "UNVERIFIED";
                score += 10;
                reasons.add("Contact details do not match any official format [+10]");
            }
        } else {
            score += 15;
            reasons.add("No verifiable contact details provided [+15]");
        }

        // ---------- RULE 10: No company name ----------
        if (company.trim().isEmpty()) {
            score += 15;
            reasons.add("No company name mentioned in the offer [+15]");
        }

        // ---------- RULE 11: Payment channel ----------
        hit = findFirst(low, PAY_CHANNEL_WORDS);
        if (hit != null) {
            score += 30;
            reasons.add("Payment requested to a personal wallet / UPI (\"" + hit + "\") instead of a company account [+30]");
        }

        // ---------- RULE 12: Link analysis ----------
        List<String> links = extractLinks(body + " " + contact);

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
                reasons.add("Link downloads an APK file - fake loan apps steal your contacts and photos [+35]");
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

        // ---------- RULE 13: Generic greeting ----------
        if (low.contains("dear candidate") || low.contains("dear applicant")
                || low.contains("dear sir/madam") || low.contains("dear user")) {
            score += 10;
            reasons.add("Generic greeting instead of your real name [+10]");
        }

        // ---------- RULE 14: Urgency ----------
        if (low.contains("urgent") || low.contains("immediately")
                || low.contains("within 24 hours") || low.contains("today only")
                || low.contains("hurry")) {
            score += 15;
            reasons.add("Creates artificial urgency to stop you verifying [+15]");
        }

        if (score > 100) {
            score = 100;
        }

        if (reasons.isEmpty()) {
            reasons.add("No known job or loan scam pattern detected");
        }

        if ("UNKNOWN".equals(scamType) && score >= 40) {
            scamType = "SUSPICIOUS OFFER";
        }
        if (score < 20) {
            scamType = "NONE DETECTED";
        }

        // ---------- VERDICT ----------
        String verdict;
        String advice;

        if (score < 20) {
            verdict = "SAFE";
            advice = "No scam indicators found. Still verify the company on LinkedIn and its official careers page before sharing documents.";
        } else if (score < 40) {
            verdict = "LOW RISK";
            advice = "A few unusual details. Confirm the recruiter on the company's official website before proceeding.";
        } else if (score < 70) {
            verdict = "SUSPICIOUS";
            advice = "Likely a scam. Do NOT pay any fee and do NOT share Aadhaar, PAN or bank details.";
        } else {
            verdict = "DANGEROUS";
            advice = "DO NOT PAY. DO NOT SHARE DOCUMENTS. No genuine employer or RBI-registered lender ever charges an upfront fee. Report on cybercrime.gov.in or call 1930.";
        }

        o.setOfferType(type);
        o.setScamType(scamType);
        o.setFeeDemanded(fee.isEmpty() ? "none detected" : fee);
        o.setRiskScore(score);
        o.setVerdict(verdict);
        o.setScannedAt(LocalDateTime.now());

        OfferScan saved = repository.save(o);
        saved.setReasons(reasons);
        saved.setLinks(links);
        saved.setAdvice(advice);
        saved.setContactType(contactType);

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

    private boolean containsAny(String text, String[] words) {
        return findFirst(text, words) != null;
    }

    // Finds a fee amount written near a fee keyword
    private String extractAmount(String body) {

        String low = safe(body).toLowerCase();

        Pattern p = Pattern.compile("(?:rs\\.?|inr|₹)\\s?([0-9][0-9,]{2,8})");
        Matcher m = p.matcher(low);

        while (m.find()) {
            int pos = m.start();
            int from = Math.max(0, pos - 70);
            int to = Math.min(low.length(), pos + 70);
            String window = low.substring(from, to);

            if (containsAny(window, ADVANCE_FEE_WORDS) || window.contains("pay")
                    || window.contains("deposit") || window.contains("fee")) {
                return m.group(1).replace(",", "");
            }
        }

        return "";
    }

    // Finds a salary figure
    private int extractSalary(String body) {

        String low = safe(body).toLowerCase();

        Pattern p = Pattern.compile("(?:rs\\.?|inr|₹)\\s?([0-9][0-9,]{3,8})");
        Matcher m = p.matcher(low);

        int best = 0;

        while (m.find()) {
            int pos = m.start();
            int from = Math.max(0, pos - 60);
            int to = Math.min(low.length(), pos + 60);
            String window = low.substring(from, to);

            if (window.contains("salary") || window.contains("per month")
                    || window.contains("monthly") || window.contains("earn")
                    || window.contains("income") || window.contains("stipend")) {
                try {
                    int v = Integer.parseInt(m.group(1).replace(",", ""));
                    if (v > best) {
                        best = v;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return best;
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

    public List<OfferScan> getAll() {
        return repository.findAll();
    }

    public Map<String, Object> getStats() {

        List<OfferScan> all = repository.findAll();

        long safeCount = 0;
        long low = 0;
        long sus = 0;
        long danger = 0;
        long jobs = 0;
        long loans = 0;

        for (OfferScan o : all) {

            String v = o.getVerdict();
            if ("SAFE".equals(v)) {
                safeCount++;
            } else if ("LOW RISK".equals(v)) {
                low++;
            } else if ("SUSPICIOUS".equals(v)) {
                sus++;
            } else if ("DANGEROUS".equals(v)) {
                danger++;
            }

            if ("LOAN".equals(o.getOfferType())) {
                loans++;
            } else {
                jobs++;
            }
        }

        Map<String, Object> m = new HashMap<>();
        m.put("total", all.size());
        m.put("safe", safeCount);
        m.put("lowRisk", low);
        m.put("suspicious", sus);
        m.put("dangerous", danger);
        m.put("threats", sus + danger);
        m.put("jobs", jobs);
        m.put("loans", loans);

        return m;
    }

    public void deleteAll() {
        repository.deleteAll();
    }
}