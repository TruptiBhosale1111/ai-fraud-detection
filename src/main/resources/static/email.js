const EAPI = "/api/email";

// ---------- SCAN ----------
function scanEmail() {

    const sender = document.getElementById("sender").value;
    const subject = document.getElementById("subject").value;
    const content = document.getElementById("content").value;

    if (content.trim() === "") {
        alert("Please paste the email text first.");
        return;
    }

    const data = {
        sender: sender,
        subject: subject,
        content: content
    };

    fetch(EAPI + "/scan", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data)
    })
    .then(res => res.json())
    .then(result => {
        showVerdict(result);
        loadEmailAll();
    })
    .catch(err => {
        alert("Cannot connect to backend. Is the Java app running?");
        console.log(err);
    });
}

// ---------- SHOW RESULT ----------
function showVerdict(e) {

    const cls = "verdict-" + e.verdict.replace(" ", "");

    let icon = "✅";
    if (e.verdict === "LOW RISK") { icon = "⚠️"; }
    if (e.verdict === "SUSPICIOUS") { icon = "🚧"; }
    if (e.verdict === "DANGEROUS") { icon = "🚨"; }

    const box = document.getElementById("verdictBox");
    box.className = "result-box " + cls;
    box.innerHTML =
        '<div class="level">' + icon + ' ' + e.verdict + '</div>' +
        '<div class="score">Risk Score: ' + e.riskScore + ' / 100</div>' +
        '<div class="score-bar"><div class="score-fill" style="width:' + e.riskScore + '%"></div></div>';

    document.getElementById("adviceBox").innerHTML =
        '<div class="advice-box">' + e.advice + '</div>';

    let html = '<div class="reason-title">Detected patterns</div>';

    e.reasons.forEach(function (r) {
        let c = "reason-item";
        if (r.indexOf("No known phishing") !== -1) {
            c = "reason-item ok";
        }
        html += '<div class="' + c + '">• ' + r + '</div>';
    });

    document.getElementById("eReasonBox").innerHTML = html;

    let linkHtml = "";
    if (e.links && e.links.length > 0) {
        linkHtml = '<div class="reason-title" style="margin-top:14px">Links found in this email</div>';
        e.links.forEach(function (l) {
            linkHtml += '<div class="link-item">🔗 ' + l + '</div>';
        });
        linkHtml += '<div class="link-warn">Never click links from an unverified sender.</div>';
    }

    document.getElementById("linkBox").innerHTML = linkHtml;
}

// ---------- STATS + HISTORY ----------
function loadEmailAll() {
    loadEmailStats();
    loadEmailList();
}

function loadEmailStats() {
    fetch(EAPI + "/stats")
    .then(res => res.json())
    .then(s => {
        document.getElementById("eTotal").innerText = s.total;
        document.getElementById("eThreats").innerText = s.threats;
        document.getElementById("eSafe").innerText = s.safe;
        document.getElementById("eDanger").innerText = s.dangerous;
    });
}

function loadEmailList() {
    fetch(EAPI)
    .then(res => res.json())
    .then(list => {

        if (list.length === 0) {
            document.getElementById("eList").innerHTML =
                '<div class="empty-msg">No emails scanned yet</div>';
            return;
        }

        list.reverse();

        let html = "<table><tr>" +
            "<th>ID</th><th>Sender</th><th>Subject</th><th>Score</th><th>Verdict</th></tr>";

        list.forEach(function (e) {

            let badge = "vb-safe";
            if (e.verdict === "LOW RISK")   { badge = "vb-low"; }
            if (e.verdict === "SUSPICIOUS") { badge = "vb-sus"; }
            if (e.verdict === "DANGEROUS")  { badge = "vb-danger"; }

            const sender = e.sender ? e.sender : "-";
            const subject = e.subject ? e.subject : "-";

            html += "<tr>" +
                "<td>" + e.id + "</td>" +
                "<td>" + sender + "</td>" +
                "<td>" + subject + "</td>" +
                "<td><b>" + e.riskScore + "</b></td>" +
                "<td><span class='badge " + badge + "'>" + e.verdict + "</span></td>" +
                "</tr>";
        });

        document.getElementById("eList").innerHTML = html + "</table>";
    });
}

function clearEmails() {
    if (!confirm("Delete all scanned emails?")) { return; }

    fetch(EAPI, { method: "DELETE" })
    .then(() => {
        loadEmailAll();
        document.getElementById("verdictBox").className = "result-empty";
        document.getElementById("verdictBox").innerText = "No email scanned yet";
        document.getElementById("adviceBox").innerHTML = "";
        document.getElementById("eReasonBox").innerHTML = "";
        document.getElementById("linkBox").innerHTML = "";
    });
}

// ---------- SAMPLES ----------
function loadScam() {
    document.getElementById("sender").value = "sbi.security.verify@gmail.com";
    document.getElementById("subject").value = "URGENT: YOUR ACCOUNT WILL BE BLOCKED";
    document.getElementById("content").value =
"Dear Customer,\n\n" +
"We have detected unusual activity on your account. Your account will be closed within 24 hours if you do not verify your account immediately.\n\n" +
"Please click here to login and confirm your identity:\n" +
"http://bit.ly/sbi-verify-now\n\n" +
"You must also share your OTP with our executive to complete the verification.\n\n" +
"Failure to act now will result in legal action.\n\n" +
"Regards,\nSBI Security Team";
}

function loadLottery() {
    document.getElementById("sender").value = "claims-dept@intl-lottery-winners.com";
    document.getElementById("subject").value = "Congratulations You Have Won 25,00,000";
    document.getElementById("content").value =
"Dear Sir/Madam,\n\n" +
"Congratulations! You have won the international lottery jackpot of Rs 25,00,000.\n\n" +
"You have been selected as a lucky winner. To claim your prize you must pay a small processing fee of Rs 8,500.\n\n" +
"Payment can be made via bitcoin or Amazon gift card to release your funds.\n\n" +
"Act now, this offer expires today.\n\n" +
"Claim here: http://192.168.44.12/claim-prize\n\n" +
"Regards,\nClaims Department";
}

function loadSafe() {
    document.getElementById("sender").value = "noreply@github.com";
    document.getElementById("subject").value = "Your monthly activity summary";
    document.getElementById("content").value =
"Hi Trupti,\n\n" +
"Here is a summary of your repository activity for this month.\n\n" +
"You made 42 commits across 3 repositories. Your most active project was fraud-detection-system.\n\n" +
"You can view the full report in your dashboard at any time by signing in normally through the website.\n\n" +
"Thanks,\nThe GitHub Team";
}

// ---------- START ----------
loadEmailAll();