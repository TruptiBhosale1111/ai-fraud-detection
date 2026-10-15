const QAPI = "/api/qr";

let camScanner = null;
let camOn = false;

// ---------- TABS ----------
function switchTab(which) {

    document.getElementById("paneP").style.display = "none";
    document.getElementById("paneI").style.display = "none";
    document.getElementById("paneC").style.display = "none";

    document.getElementById("tabPaste").className = "tab-btn";
    document.getElementById("tabImage").className = "tab-btn";
    document.getElementById("tabCam").className = "tab-btn";

    if (which === "paste") {
        document.getElementById("paneP").style.display = "block";
        document.getElementById("tabPaste").className = "tab-btn active";
    } else if (which === "image") {
        document.getElementById("paneI").style.display = "block";
        document.getElementById("tabImage").className = "tab-btn active";
    } else {
        document.getElementById("paneC").style.display = "block";
        document.getElementById("tabCam").className = "tab-btn active";
    }
}

// ---------- SEND TO BACKEND ----------
function scanQR() {

    const data = document.getElementById("qrData").value.trim();

    if (data === "") {
        alert("Paste the QR data first, or upload a QR image.");
        return;
    }

    fetch(QAPI + "/scan", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ rawData: data })
    })
    .then(res => res.json())
    .then(result => {
        showQR(result);
        loadQRAll();
    })
    .catch(err => {
        alert("Cannot connect to backend. Is the Java app running?");
        console.log(err);
    });
}

// ---------- SHOW RESULT ----------
function showQR(q) {

    const cls = "verdict-" + q.verdict.replace(" ", "");

    let icon = "✅";
    if (q.verdict === "LOW RISK")   { icon = "⚠️"; }
    if (q.verdict === "SUSPICIOUS") { icon = "🚧"; }
    if (q.verdict === "DANGEROUS")  { icon = "🚨"; }

    const box = document.getElementById("qVerdict");
    box.className = "result-box " + cls;
    box.innerHTML =
        '<div class="level">' + icon + ' ' + q.verdict + '</div>' +
        '<div class="score">Risk Score: ' + q.riskScore + ' / 100</div>' +
        '<div class="score-bar"><div class="score-fill" style="width:' + q.riskScore + '%"></div></div>';

    document.getElementById("qAdvice").innerHTML =
        '<div class="advice-box">' + q.advice + '</div>';

    let d = '<div class="reason-title" style="margin-top:14px">Decoded QR details</div>';
    d += '<table class="detail-table">';
    d += '<tr><td>Type</td><td><b>' + q.qrType + '</b></td></tr>';
    d += '<tr><td>UPI ID</td><td>' + (q.payeeVpa ? q.payeeVpa : "—") + '</td></tr>';
    d += '<tr><td>Payee Name</td><td>' + (q.payeeName ? q.payeeName : "—") + '</td></tr>';
    d += '<tr><td>Amount</td><td>' + (q.amount ? "Rs " + q.amount : "not set") + '</td></tr>';
    d += '</table>';

    document.getElementById("qDetails").innerHTML = d;

    let html = '<div class="reason-title" style="margin-top:14px">Detected patterns</div>';

    q.reasons.forEach(function (r) {
        let c = "reason-item";
        if (r.indexOf("no red flags") !== -1) {
            c = "reason-item ok";
        }
        html += '<div class="' + c + '">• ' + r + '</div>';
    });

    document.getElementById("qReasons").innerHTML = html;
}

// ---------- IMAGE UPLOAD ----------
function readImage() {

    const f = document.getElementById("qrFile");
    const status = document.getElementById("imgStatus");

    if (f.files.length === 0) {
        return;
    }

    status.className = "qr-status loading";
    status.innerText = "Decoding image...";

    const reader = new Html5Qrcode("reader-file");

    reader.scanFile(f.files[0], true)
    .then(function (decoded) {
        status.className = "qr-status ok";
        status.innerText = "✔ Decoded: " + decoded;
        document.getElementById("qrData").value = decoded;
        scanQR();
    })
    .catch(function (err) {
        status.className = "qr-status err";
        status.innerText = "Could not read a QR code from this image.";
        console.log(err);
    });
}

// ---------- CAMERA ----------
function toggleCam() {

    const btn = document.getElementById("camBtn");

    if (camOn) {
        camScanner.stop().then(function () {
            camOn = false;
            btn.innerText = "📷 Start Camera";
        });
        return;
    }

    camScanner = new Html5Qrcode("reader");

    camScanner.start(
        { facingMode: "environment" },
        { fps: 10, qrbox: 240 },
        function (decoded) {
            document.getElementById("qrData").value = decoded;
            camScanner.stop().then(function () {
                camOn = false;
                btn.innerText = "📷 Start Camera";
                scanQR();
            });
        },
        function () { }
    )
    .then(function () {
        camOn = true;
        btn.innerText = "⏹ Stop Camera";
    })
    .catch(function (err) {
        alert("Camera not available.\nUse Paste or Upload Image instead.");
        console.log(err);
    });
}

// ---------- QR PREVIEW ----------
function makePreview() {

    const data = document.getElementById("qrData").value.trim();

    if (data === "") {
        alert("Enter QR data first.");
        return;
    }

    const url = "https://api.qrserver.com/v1/create-qr-code/?size=190x190&data="
              + encodeURIComponent(data);

    document.getElementById("qrPreview").innerHTML =
        '<div class="qr-img-wrap"><img src="' + url + '" alt="QR"></div>';
}

// ---------- STATS + HISTORY ----------
function loadQRAll() {
    loadQRStats();
    loadQRList();
}

function loadQRStats() {
    fetch(QAPI + "/stats")
    .then(res => res.json())
    .then(s => {
        document.getElementById("qTotal").innerText = s.total;
        document.getElementById("qThreats").innerText = s.threats;
        document.getElementById("qSafe").innerText = s.safe;
        document.getElementById("qDanger").innerText = s.dangerous;
    });
}

function loadQRList() {
    fetch(QAPI)
    .then(res => res.json())
    .then(list => {

        if (list.length === 0) {
            document.getElementById("qList").innerHTML =
                '<div class="empty-msg">No QR codes scanned yet</div>';
            return;
        }

        list.reverse();

        let html = "<table><tr>" +
            "<th>ID</th><th>Type</th><th>UPI ID</th><th>Payee</th>" +
            "<th>Amount</th><th>Score</th><th>Verdict</th></tr>";

        list.forEach(function (q) {

            let badge = "vb-safe";
            if (q.verdict === "LOW RISK")   { badge = "vb-low"; }
            if (q.verdict === "SUSPICIOUS") { badge = "vb-sus"; }
            if (q.verdict === "DANGEROUS")  { badge = "vb-danger"; }

            html += "<tr>" +
                "<td>" + q.id + "</td>" +
                "<td>" + q.qrType + "</td>" +
                "<td>" + (q.payeeVpa ? q.payeeVpa : "—") + "</td>" +
                "<td>" + (q.payeeName ? q.payeeName : "—") + "</td>" +
                "<td>" + (q.amount ? "Rs " + q.amount : "—") + "</td>" +
                "<td><b>" + q.riskScore + "</b></td>" +
                "<td><span class='badge " + badge + "'>" + q.verdict + "</span></td>" +
                "</tr>";
        });

        document.getElementById("qList").innerHTML = html + "</table>";
    });
}

function clearQR() {
    if (!confirm("Delete all QR scans?")) { return; }

    fetch(QAPI, { method: "DELETE" })
    .then(() => {
        loadQRAll();
        document.getElementById("qVerdict").className = "result-empty";
        document.getElementById("qVerdict").innerText = "No QR analyzed yet";
        document.getElementById("qAdvice").innerHTML = "";
        document.getElementById("qDetails").innerHTML = "";
        document.getElementById("qReasons").innerHTML = "";
        document.getElementById("qrPreview").innerHTML = "";
    });
}

// ---------- SAMPLES ----------
function sampleSafe() {
    document.getElementById("qrData").value =
        "upi://pay?pa=sharmastores@oksbi&pn=Sharma General Stores&cu=INR&mc=5411";
}

function sampleRefund() {
    document.getElementById("qrData").value =
        "upi://pay?pa=refund.helpdesk@paymentz&pn=&am=14999&cu=INR&tn=Scan to receive your refund of Rs 14999";
}

function sampleCollect() {
    document.getElementById("qrData").value =
        "upi://collect?pa=olx.buyer.army@axisbnk&pn=Army Officer&am=25000&cu=INR&mode=02&tn=Advance payment claim";
}

function samplePhish() {
    document.getElementById("qrData").value =
        "http://bit.ly/upi-kyc-verify-now";
}

// ---------- START ----------
loadQRAll();