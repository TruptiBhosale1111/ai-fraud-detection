const OAPI = "/api/offers";

// ---------- SCAN ----------
function scanOffer() {

    const offerType = document.getElementById("oType").value;
    const companyName = document.getElementById("oCompany").value;
    const contactInfo = document.getElementById("oContact").value;
    const content = document.getElementById("oContent").value;

    if (content.trim() === "") {
        alert("Paste the offer message first.");
        return;
    }

    const data = {
        offerType: offerType,
        companyName: companyName,
        contactInfo: contactInfo,
        content: content
    };

    fetch(OAPI + "/scan", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data)
    })
    .then(res => res.json())
    .then(result => {
        showOffer(result);
        loadOfferAll();
    })
    .catch(err => {
        alert("Cannot connect to backend. Is the Java app running?");
        console.log(err);
    });
}

// ---------- SHOW RESULT ----------
function showOffer(o) {

    const cls = "verdict-" + o.verdict.replace(" ", "");

    let icon = "✅";
    if (o.verdict === "LOW RISK")   { icon = "⚠️"; }
    if (o.verdict === "SUSPICIOUS") { icon = "🚧"; }
    if (o.verdict === "DANGEROUS")  { icon = "🚨"; }

    const box = document.getElementById("oVerdict");
    box.className = "result-box " + cls;
    box.innerHTML =
        '<div class="level">' + icon + ' ' + o.verdict + '</div>' +
        '<div class="score">Risk Score: ' + o.riskScore + ' / 100</div>' +
        '<div class="score-bar"><div class="score-fill" style="width:' + o.riskScore + '%"></div></div>';

    document.getElementById("oAdvice").innerHTML =
        '<div class="advice-box">' + o.advice + '</div>';

    let meta = '<div class="reason-title" style="margin-top:14px">Offer analysis</div>';
    meta += '<table class="detail-table">';
    meta += '<tr><td>Scam Type</td><td><b>' + o.scamType + '</b></td></tr>';
    meta += '<tr><td>Offer Type</td><td>' + o.offerType + '</td></tr>';
    meta += '<tr><td>Company</td><td>' + (o.companyName ? o.companyName : "not given") + '</td></tr>';
    meta += '<tr><td>Contact Type</td><td>' + o.contactType + '</td></tr>';
    meta += '<tr><td>Fee Demanded</td><td><b>' + (o.feeDemanded === "none detected" ? "none detected" : "Rs " + o.feeDemanded) + '</b></td></tr>';
    meta += '</table>';

    document.getElementById("oMeta").innerHTML = meta;

    let html = '<div class="reason-title" style="margin-top:14px">Detected patterns</div>';

    o.reasons.forEach(function (r) {
        let cl = "reason-item";
        if (r.indexOf("No known job or loan") !== -1) {
            cl = "reason-item ok";
        }
        html += '<div class="' + cl + '">• ' + r + '</div>';
    });

    document.getElementById("oReasons").innerHTML = html;

    let linkHtml = "";
    if (o.links && o.links.length > 0) {
        linkHtml = '<div class="reason-title" style="margin-top:14px">Links in this offer</div>';
        o.links.forEach(function (l) {
            linkHtml += '<div class="link-item">🔗 ' + l + '</div>';
        });
        linkHtml += '<div class="link-warn">Never install an app or log in through a link sent by a recruiter.</div>';
    }

    document.getElementById("oLinks").innerHTML = linkHtml;
}

// ---------- STATS + HISTORY ----------
function loadOfferAll() {
    loadOfferStats();
    loadOfferList();
}

function loadOfferStats() {
    fetch(OAPI + "/stats")
    .then(res => res.json())
    .then(s => {
        document.getElementById("oTotal").innerText = s.total;
        document.getElementById("oThreats").innerText = s.threats;
        document.getElementById("oSafe").innerText = s.safe;
        document.getElementById("oDanger").innerText = s.dangerous;
        document.getElementById("oSplit").innerText = s.jobs + " / " + s.loans;
    });
}

function loadOfferList() {
    fetch(OAPI)
    .then(res => res.json())
    .then(list => {

        if (list.length === 0) {
            document.getElementById("oList").innerHTML =
                '<div class="empty-msg">No offers checked yet</div>';
            return;
        }

        list.reverse();

        let html = "<table><tr>" +
            "<th>ID</th><th>Type</th><th>Company</th><th>Scam Type</th>" +
            "<th>Fee</th><th>Score</th><th>Verdict</th></tr>";

        list.forEach(function (o) {

            let badge = "vb-safe";
            if (o.verdict === "LOW RISK")   { badge = "vb-low"; }
            if (o.verdict === "SUSPICIOUS") { badge = "vb-sus"; }
            if (o.verdict === "DANGEROUS")  { badge = "vb-danger"; }

            const feeTxt = (o.feeDemanded === "none detected") ? "—" : "Rs " + o.feeDemanded;

            html += "<tr>" +
                "<td>" + o.id + "</td>" +
                "<td>" + o.offerType + "</td>" +
                "<td>" + (o.companyName ? o.companyName : "—") + "</td>" +
                "<td>" + o.scamType + "</td>" +
                "<td>" + feeTxt + "</td>" +
                "<td><b>" + o.riskScore + "</b></td>" +
                "<td><span class='badge " + badge + "'>" + o.verdict + "</span></td>" +
                "</tr>";
        });

        document.getElementById("oList").innerHTML = html + "</table>";
    });
}

function clearOffers() {
    if (!confirm("Delete all offer checks?")) { return; }

    fetch(OAPI, { method: "DELETE" })
    .then(() => {
        loadOfferAll();
        document.getElementById("oVerdict").className = "result-empty";
        document.getElementById("oVerdict").innerText = "No offer checked yet";
        document.getElementById("oAdvice").innerHTML = "";
        document.getElementById("oMeta").innerHTML = "";
        document.getElementById("oReasons").innerHTML = "";
        document.getElementById("oLinks").innerHTML = "";
    });
}

// ---------- SAMPLES ----------
function sFeeJob() {
    document.getElementById("oType").value = "JOB";
    document.getElementById("oCompany").value = "TCS Recruitment Cell";
    document.getElementById("oContact").value = "tcs.hr.selection2026@gmail.com";
    document.getElementById("oContent").value =
"Dear Candidate,\n\n" +
"Congratulations! Your resume is shortlisted from the portal for the post of Junior Software Engineer at TCS.\n" +
"You have been selected without interview based on your profile.\n\n" +
"Salary: Rs 45,000 per month. Direct joining.\n\n" +
"To confirm your seat you must pay a refundable security deposit of Rs 4,500 towards ID card and documentation charge. " +
"Limited seats available, confirm within 24 hours.\n\n" +
"Payment can be made on Google Pay to 9876543210.\n" +
"Also share your Aadhaar card photo and cancelled cheque for onboarding.\n\n" +
"Regards,\nHR Team";
}

function sTask() {
    document.getElementById("oType").value = "WORK FROM HOME";
    document.getElementById("oCompany").value = "";
    document.getElementById("oContact").value = "https://t.me/earndaily_officialtask";
    document.getElementById("oContent").value =
"Hello! Part time job available. Work from home job, no experience required, students and housewives can apply.\n\n" +
"Just complete simple YouTube like and subscribe task and earn daily Rs 3,000.\n" +
"Daily payout guaranteed income.\n\n" +
"Join our whatsapp group and telegram channel to start.\n" +
"First you need to do a small prepaid task, deposit to unlock your commission task.\n\n" +
"Register here: http://bit.ly/earn-daily-task-join";
}

function sLoan() {
    document.getElementById("oType").value = "LOAN";
    document.getElementById("oCompany").value = "QuickCash Instant Finance";
    document.getElementById("oContact").value = "9123456780";
    document.getElementById("oContent").value =
"Dear Sir/Madam,\n\n" +
"Your instant loan of Rs 3,00,000 is pre-approved and sanctioned!\n" +
"No credit check, loan without documents, bad CIBIL ok. Disbursal in 5 minutes.\n\n" +
"To release the amount you must pay a processing fee of Rs 2,999 and insurance charge immediately.\n" +
"Pay on PhonePe to the number given above.\n\n" +
"Download our app to continue: http://quickcash-loan-apply.com/app.apk\n\n" +
"Offer expires today. Hurry!";
}

function sFakeHR() {
    document.getElementById("oType").value = "INTERNSHIP";
    document.getElementById("oCompany").value = "Amazon India";
    document.getElementById("oContact").value = "amazon.hiring.official@gmail.com";
    document.getElementById("oContent").value =
"Dear Applicant,\n\n" +
"Amazon India is hiring interns urgently. You are selected directly, no interview required.\n\n" +
"Stipend Rs 55,000 per month, work from home, no skills required.\n\n" +
"Kindly share your Aadhaar card photo, PAN card photo and bank passbook photo along with your OTP " +
"so we can activate your salary account.\n\n" +
"A one time registration fee of Rs 1,200 is applicable for the joining kit.\n\n" +
"Regards,\nAmazon HR";
}

function sGenuine() {
    document.getElementById("oType").value = "JOB";
    document.getElementById("oCompany").value = "Infosys Limited";
    document.getElementById("oContact").value = "careers@infosys.com";
    document.getElementById("oContent").value =
"Hello Trupti,\n\n" +
"Thank you for applying to the Systems Engineer role through our careers portal.\n\n" +
"Your application has moved to the next stage. We would like to schedule a technical interview " +
"on 25 September at 11:00 AM. The interview will be conducted over our official video platform " +
"and the link will be shared from our HR system.\n\n" +
"Please carry your original academic documents to the final round. There is no fee at any stage " +
"of our recruitment process.\n\n" +
"You may verify this opening on our official careers page.\n\n" +
"Regards,\nTalent Acquisition Team\nInfosys Limited";
}

// ---------- START ----------
loadOfferAll();