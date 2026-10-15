const CAPI = "/api/calls";

// ---------- SCAN ----------
function scanCall() {

    const sender = document.getElementById("cSender").value;
    const channel = document.getElementById("cChannel").value;
    const message = document.getElementById("cMessage").value;

    if (message.trim() === "") {
        alert("Paste the message text first.");
        return;
    }

    const data = {
        sender: sender,
        channel: channel,
        message: message
    };

    fetch(CAPI + "/scan", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data)
    })
    .then(res => res.json())
    .then(result => {
        showCall(result);
        loadCallAll();
    })
    .catch(err => {
        alert("Cannot connect to backend. Is the Java app running?");
        console.log(err);
    });
}

// ---------- SHOW RESULT ----------
function showCall(c) {

    const cls = "verdict-" + c.verdict.replace(" ", "");

    let icon = "✅";
    if (c.verdict === "LOW RISK")   { icon = "⚠️"; }
    if (c.verdict === "SUSPICIOUS") { icon = "🚧"; }
    if (c.verdict === "DANGEROUS")  { icon = "🚨"; }

    const box = document.getElementById("cVerdict");
    box.className = "result-box " + cls;
    box.innerHTML =
        '<div class="level">' + icon + ' ' + c.verdict + '</div>' +
        '<div class="score">Risk Score: ' + c.riskScore + ' / 100</div>' +
        '<div class="score-bar"><div class="score-fill" style="width:' + c.riskScore + '%"></div></div>';

    document.getElementById("cAdvice").innerHTML =
        '<div class="advice-box">' + c.advice + '</div>';

    let meta = '<div class="reason-title" style="margin-top:14px">Identified as</div>';
    meta += '<table class="detail-table">';
    meta += '<tr><td>Scam Type</td><td><b>' + c.scamType + '</b></td></tr>';
    meta += '<tr><td>Channel</td><td>' + c.channel + '</td></tr>';
    meta += '<tr><td>Sender Type</td><td>' + c.senderType + '</td></tr>';
    meta += '<tr><td>Sender</td><td>' + (c.sender ? c.sender : "—") + '</td></tr>';
    meta += '</table>';

    document.getElementById("cMeta").innerHTML = meta;

    let html = '<div class="reason-title" style="margin-top:14px">Detected patterns</div>';

    c.reasons.forEach(function (r) {
        let cl = "reason-item";
        if (r.indexOf("No known scam") !== -1) {
            cl = "reason-item ok";
        }
        html += '<div class="' + cl + '">• ' + r + '</div>';
    });

    document.getElementById("cReasons").innerHTML = html;

    let linkHtml = "";
    if (c.links && c.links.length > 0) {
        linkHtml = '<div class="reason-title" style="margin-top:14px">Links in this message</div>';
        c.links.forEach(function (l) {
            linkHtml += '<div class="link-item">🔗 ' + l + '</div>';
        });
        linkHtml += '<div class="link-warn">Never tap links sent by an unknown number.</div>';
    }

    document.getElementById("cLinks").innerHTML = linkHtml;
}

// ---------- STATS + HISTORY ----------
function loadCallAll() {
    loadCallStats();
    loadCallList();
}

function loadCallStats() {
    fetch(CAPI + "/stats")
    .then(res => res.json())
    .then(s => {
        document.getElementById("cTotal").innerText = s.total;
        document.getElementById("cThreats").innerText = s.threats;
        document.getElementById("cSafe").innerText = s.safe;
        document.getElementById("cDanger").innerText = s.dangerous;
    });
}

function loadCallList() {
    fetch(CAPI)
    .then(res => res.json())
    .then(list => {

        if (list.length === 0) {
            document.getElementById("cList").innerHTML =
                '<div class="empty-msg">No messages checked yet</div>';
            return;
        }

        list.reverse();

        let html = "<table><tr>" +
            "<th>ID</th><th>Channel</th><th>Sender</th><th>Scam Type</th>" +
            "<th>Score</th><th>Verdict</th></tr>";

        list.forEach(function (c) {

            let badge = "vb-safe";
            if (c.verdict === "LOW RISK")   { badge = "vb-low"; }
            if (c.verdict === "SUSPICIOUS") { badge = "vb-sus"; }
            if (c.verdict === "DANGEROUS")  { badge = "vb-danger"; }

            html += "<tr>" +
                "<td>" + c.id + "</td>" +
                "<td>" + c.channel + "</td>" +
                "<td>" + (c.sender ? c.sender : "—") + "</td>" +
                "<td>" + c.scamType + "</td>" +
                "<td><b>" + c.riskScore + "</b></td>" +
                "<td><span class='badge " + badge + "'>" + c.verdict + "</span></td>" +
                "</tr>";
        });

        document.getElementById("cList").innerHTML = html + "</table>";
    });
}

function clearCalls() {
    if (!confirm("Delete all scan history?")) { return; }

    fetch(CAPI, { method: "DELETE" })
    .then(() => {
        loadCallAll();
        document.getElementById("cVerdict").className = "result-empty";
        document.getElementById("cVerdict").innerText = "No message checked yet";
        document.getElementById("cAdvice").innerHTML = "";
        document.getElementById("cMeta").innerHTML = "";
        document.getElementById("cReasons").innerHTML = "";
        document.getElementById("cLinks").innerHTML = "";
    });
}

// ---------- SAMPLES ----------
function sKyc() {
    document.getElementById("cSender").value = "+919812345678";
    document.getElementById("cChannel").value = "SMS";
    document.getElementById("cMessage").value =
"Dear Customer, your SBI account will be blocked today as your KYC is expired. " +
"Update your KYC immediately to avoid suspension. " +
"Click here: http://bit.ly/sbi-kyc-update " +
"Or call on 9812345678. - SBI Bank";
}

function sArrest() {
    document.getElementById("cSender").value = "+9231455512";
    document.getElementById("cChannel").value = "CALL";
    document.getElementById("cMessage").value =
"Sir, this is from CBI Mumbai cyber crime branch. Your parcel contains illegal item and narcotics. " +
"An FIR has been registered against you for money laundering. " +
"You are under digital arrest. Do not disconnect the call. " +
"Join video call for verification and transfer the amount to the verification account immediately " +
"otherwise arrest warrant will be issued.";
}

function sLottery() {
    document.getElementById("cSender").value = "+2348123456789";
    document.getElementById("cChannel").value = "WHATSAPP";
    document.getElementById("cMessage").value =
"Congratulations! You have won Rs 25,00,000 in KBC lucky draw lottery. " +
"You are our lucky winner selected from all India. " +
"To claim your prize pay processing fee of Rs 6,500 immediately. " +
"Send details on http://192.168.10.55/kbc-claim. Offer expires today.";
}

function sElectric() {
    document.getElementById("cSender").value = "+919988776655";
    document.getElementById("cChannel").value = "SMS";
    document.getElementById("cMessage").value =
"Dear Consumer, your electricity connection will be disconnected tonight at 9:30 PM " +
"because your previous month bill was not updated. " +
"Please immediately contact us at 9988776655 for update. - Electricity Office";
}

function sArmy() {
    document.getElementById("cSender").value = "+917700112233";
    document.getElementById("cChannel").value = "WHATSAPP";
    document.getElementById("cMessage").value =
"Hello sir, I am an army officer posted at Jammu military camp. " +
"I saw your OLX ad. I am transferred urgently so I will buy it without seeing. " +
"I will send advance payment through army canteen portal. " +
"Please install anydesk app so I can help you complete the payment. " +
"Share the OTP you receive to confirm the transfer.";
}

function sSafe() {
    document.getElementById("cSender").value = "VM-HDFCBK";
    document.getElementById("cChannel").value = "SMS";
    document.getElementById("cMessage").value =
"Rs 450.00 debited from a/c XX4521 on 19-Sep-26 to VPA sharmastores@oksbi. " +
"Not you? Report at 18002026161. -HDFC Bank";
}

// ---------- START ----------
loadCallAll();