const API = "/api/transactions";

let allData = [];
let pieChart = null;
let barChart = null;

// ---------- ANALYZE ----------
function analyze() {

    const userId = document.getElementById("userId").value;
    const amount = document.getElementById("amount").value;
    const type = document.getElementById("type").value;
    const location = document.getElementById("location").value;
    const time = document.getElementById("time").value;

    if (amount === "" || location === "" || time === "") {
        alert("Please fill all fields");
        return;
    }

    const data = {
        userId: userId,
        amount: parseFloat(amount),
        transactionType: type,
        location: location,
        transactionTime: time
    };

    fetch(API + "/analyze", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data)
    })
    .then(res => res.json())
    .then(result => {
        showResult(result);
        loadAll();
    })
    .catch(err => {
        alert("Cannot connect to backend. Is the Java app running?");
        console.log(err);
    });
}

function showResult(tx) {

    const level = tx.riskLevel;
    const cssClass = "level-" + level.replace(" ", "");

    const box = document.getElementById("resultBox");
    box.className = "result-box " + cssClass;
    box.innerHTML =
        '<div class="level">' + level + '</div>' +
        '<div class="score">User: ' + tx.userId + ' &nbsp;|&nbsp; Result: ' + tx.isFraud +
        ' &nbsp;|&nbsp; Score: ' + tx.fraudScore + ' / 100</div>' +
        '<div class="score-bar"><div class="score-fill" style="width:' + tx.fraudScore + '%"></div></div>';

    let html = '<div class="reason-title">Why this decision?</div>';

    tx.reasons.forEach(function (r) {
        let cls = "reason-item";
        if (r.indexOf("No suspicious") !== -1) {
            cls = "reason-item ok";
        }
        html += '<div class="' + cls + '">• ' + r + '</div>';
    });

    document.getElementById("reasonBox").innerHTML = html;
}

// ---------- REVIEW / LABEL ----------
function setLabel(id, value) {

    fetch(API + "/" + id + "/label?value=" + value, { method: "PUT" })
    .then(res => res.json())
    .then(() => {
        loadTransactions();
        loadMetrics();
    })
    .catch(err => {
        alert("Could not save label.");
        console.log(err);
    });
}

// ---------- METRICS ----------
function loadMetrics() {

    fetch(API + "/metrics")
    .then(res => res.json())
    .then(m => {

        const box = document.getElementById("metricsBox");

        if (m.labeled === 0) {
            box.innerHTML =
                '<div class="no-label-msg">' +
                'No transactions reviewed yet.<br>' +
                'Use the <b>Fraud</b> / <b>Legit</b> buttons in the table below to provide ground truth labels.' +
                '</div>';
            return;
        }

        let html =
            '<div class="metric-grid">' +
            '<div class="metric-box m-acc"><div class="mnum">' + m.accuracy + '%</div><div class="mtxt">Accuracy</div></div>' +
            '<div class="metric-box m-prec"><div class="mnum">' + m.precision + '%</div><div class="mtxt">Precision</div></div>' +
            '<div class="metric-box m-rec"><div class="mnum">' + m.recall + '%</div><div class="mtxt">Recall</div></div>' +
            '<div class="metric-box m-f1"><div class="mnum">' + m.f1Score + '%</div><div class="mtxt">F1 Score</div></div>' +
            '<div class="metric-box m-spec"><div class="mnum">' + m.specificity + '%</div><div class="mtxt">Specificity</div></div>' +
            '</div>';

        html +=
            '<div class="matrix-wrap">' +
            '<div class="matrix-title">Confusion Matrix &nbsp;(' + m.labeled + ' reviewed, ' + m.unlabeled + ' pending)</div>' +
            '<div class="matrix">' +

            '<div class="mcell mhead"></div>' +
            '<div class="mcell mhead">Actual: FRAUD</div>' +
            '<div class="mcell mhead">Actual: LEGIT</div>' +

            '<div class="mcell mhead">Predicted<br>FRAUD</div>' +
            '<div class="mcell c-tp"><span class="cnum">' + m.truePositive + '</span>True Positive</div>' +
            '<div class="mcell c-fp"><span class="cnum">' + m.falsePositive + '</span>False Positive</div>' +

            '<div class="mcell mhead">Predicted<br>LEGIT</div>' +
            '<div class="mcell c-fn"><span class="cnum">' + m.falseNegative + '</span>False Negative</div>' +
            '<div class="mcell c-tn"><span class="cnum">' + m.trueNegative + '</span>True Negative</div>' +

            '</div></div>';

        box.innerHTML = html;
    });
}

// ---------- USER PROFILE ----------
function loadProfile() {

    const user = document.getElementById("profileUser").value.trim();

    if (user === "") {
        alert("Enter a User ID");
        return;
    }

    fetch(API + "/profile/" + user)
    .then(res => res.json())
    .then(p => {

        const statusClass = "st-" + p.status.replace(/ /g, "");

        let html =
            '<div class="profile-grid">' +
            '<div class="profile-box"><div class="pnum">' + p.txCount + '</div><div class="ptxt">Transactions</div></div>' +
            '<div class="profile-box"><div class="pnum">Rs ' + p.avgAmount + '</div><div class="ptxt">Avg Amount</div></div>' +
            '<div class="profile-box"><div class="pnum">Rs ' + p.maxAmount + '</div><div class="ptxt">Max Amount</div></div>' +
            '<div class="profile-box"><div class="pnum">' + p.fraudCount + '</div><div class="ptxt">Frauds</div></div>' +
            '<div class="profile-box"><div class="pnum">' + p.locations.length + '</div><div class="ptxt">Known Places</div></div>' +
            '</div>';

        html += '<div><span class="profile-status ' + statusClass + '">' + p.status + '</span></div>';

        if (p.locations.length > 0) {
            html += '<div class="loc-list"><b>Known locations:</b><br>';
            p.locations.forEach(function (l) {
                html += '<span class="loc-chip">' + l + '</span>';
            });
            html += '</div>';
        }

        document.getElementById("profileBox").innerHTML = html;
    });
}

// ---------- TRAIN DEMO USER ----------
function trainUser() {

    const training = [
        { userId: "USER001", amount: 450, transactionType: "PURCHASE", location: "Mumbai", transactionTime: "2026-01-10T11:00:00" },
        { userId: "USER001", amount: 620, transactionType: "PURCHASE", location: "Mumbai", transactionTime: "2026-01-11T15:30:00" },
        { userId: "USER001", amount: 380, transactionType: "PURCHASE", location: "Mumbai", transactionTime: "2026-01-12T18:10:00" },
        { userId: "USER001", amount: 550, transactionType: "PURCHASE", location: "Mumbai", transactionTime: "2026-01-13T13:45:00" }
    ];

    fetch(API + "/analyze-batch", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(training)
    })
    .then(res => res.json())
    .then(() => {
        alert("USER001 trained with 4 normal transactions (avg Rs 500, Mumbai).\n\nNow try Rs 4000 in Mumbai at 2 PM.");
        document.getElementById("profileUser").value = "USER001";
        loadProfile();
        loadAll();
    });
}

// ---------- CSV UPLOAD ----------
function uploadCSV() {

    const fileInput = document.getElementById("csvFile");
    const status = document.getElementById("uploadStatus");

    if (fileInput.files.length === 0) {
        status.className = "status-error";
        status.innerText = "Please choose a CSV file first.";
        return;
    }

    const reader = new FileReader();

    reader.onload = function (e) {

        const lines = e.target.result.split("\n");
        const list = [];
        let skipped = 0;

        for (let i = 0; i < lines.length; i++) {

            let line = lines[i].trim();
            if (line === "") { continue; }
            if (i === 0 && line.toLowerCase().indexOf("amount") !== -1) { continue; }

            const p = line.split(",");

            if (p.length >= 5) {
                const amt = parseFloat(p[1].trim());
                if (isNaN(amt)) { skipped++; continue; }
                list.push({
                    userId: p[0].trim(),
                    amount: amt,
                    transactionType: p[2].trim().toUpperCase(),
                    location: p[3].trim(),
                    transactionTime: p[4].trim()
                });
            } else if (p.length === 4) {
                const amt = parseFloat(p[0].trim());
                if (isNaN(amt)) { skipped++; continue; }
                list.push({
                    userId: "GUEST",
                    amount: amt,
                    transactionType: p[1].trim().toUpperCase(),
                    location: p[2].trim(),
                    transactionTime: p[3].trim()
                });
            } else {
                skipped++;
            }
        }

        if (list.length === 0) {
            status.className = "status-error";
            status.innerText = "No valid rows found.";
            return;
        }

        status.className = "status-loading";
        status.innerText = "Analyzing " + list.length + " transactions...";

        fetch(API + "/analyze-batch", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(list)
        })
        .then(res => res.json())
        .then(results => {
            status.className = "status-ok";
            status.innerText = "✔ Analyzed " + results.length + " transactions."
                + (skipped > 0 ? " (" + skipped + " skipped)" : "");
            showBatchSummary(results);
            loadAll();
        })
        .catch(err => {
            status.className = "status-error";
            status.innerText = "Upload failed. Is the backend running?";
            console.log(err);
        });
    };

    reader.readAsText(fileInput.files[0]);
}

function showBatchSummary(results) {

    let fraud = 0, legit = 0, high = 0, sus = 0;

    results.forEach(function (t) {
        if (t.isFraud === "FRAUD") { fraud++; } else { legit++; }
        if (t.riskLevel === "HIGH RISK") { high++; }
        if (t.riskLevel === "SUSPICIOUS") { sus++; }
    });

    document.getElementById("batchSummary").innerHTML =
        '<div class="batch-grid">' +
        '<div class="batch-box bx-total"><div class="num">' + results.length + '</div><div class="txt">Processed</div></div>' +
        '<div class="batch-box bx-fraud"><div class="num">' + fraud + '</div><div class="txt">Fraud</div></div>' +
        '<div class="batch-box bx-legit"><div class="num">' + legit + '</div><div class="txt">Legit</div></div>' +
        '<div class="batch-box bx-high"><div class="num">' + high + '</div><div class="txt">High Risk</div></div>' +
        '<div class="batch-box bx-sus"><div class="num">' + sus + '</div><div class="txt">Suspicious</div></div>' +
        '</div>';
}

function downloadSample() {

    const csv =
"userId,amount,transactionType,location,transactionTime\n" +
"USER001,450,PURCHASE,Mumbai,2026-01-10T11:00:00\n" +
"USER001,620,PURCHASE,Mumbai,2026-01-11T15:30:00\n" +
"USER001,380,PURCHASE,Mumbai,2026-01-12T18:10:00\n" +
"USER001,550,PURCHASE,Mumbai,2026-01-13T13:45:00\n" +
"USER001,4000,PURCHASE,Mumbai,2026-01-14T14:00:00\n" +
"USER001,8000,WITHDRAWAL,Dubai,2026-01-14T16:00:00\n" +
"USER002,12000,PURCHASE,Delhi,2026-01-10T12:00:00\n" +
"USER002,15000,PURCHASE,Delhi,2026-01-11T13:00:00\n" +
"USER002,11000,PURCHASE,Delhi,2026-01-12T14:00:00\n" +
"USER002,13000,PURCHASE,Delhi,2026-01-13T15:00:00\n" +
"USER003,500,PURCHASE,Pune,2026-01-15T10:00:00\n" +
"USER003,700,PURCHASE,Pune,2026-01-15T12:00:00\n" +
"USER003,600,PURCHASE,Pune,2026-01-15T14:00:00\n" +
"USER003,25000,WITHDRAWAL,UNKNOWN,2026-01-16T03:00:00\n" +
"USER004,2000,WITHDRAWAL,Chennai,2026-01-16T11:00:00\n" +
"USER004,18000,WITHDRAWAL,FOREIGN-Bangkok,2026-01-17T02:45:00\n" +
"USER005,750,PURCHASE,Jaipur,2026-01-17T12:00:00\n" +
"USER005,300,PURCHASE,Surat,2026-01-18T19:20:00\n" +
"USER005,9500,TRANSFER,HIGH-RISK Zone,2026-01-19T23:00:00\n" +
"USER005,20000,TRANSFER,UNKNOWN,2026-01-20T01:15:00\n";

    downloadFile(csv, "sample_transactions.csv");
}

function exportCSV() {

    if (allData.length === 0) {
        alert("No transactions to export.");
        return;
    }

    let csv = "ID,UserID,Amount,Type,Location,Time,Score,RiskLevel,Predicted,ActualLabel\n";

    allData.forEach(function (t) {
        csv += t.id + "," + t.userId + "," + t.amount + "," + t.transactionType + "," +
               t.location + "," + t.transactionTime + "," + t.fraudScore + "," +
               t.riskLevel + "," + t.isFraud + "," + (t.actualLabel ? t.actualLabel : "NOT_REVIEWED") + "\n";
    });

    downloadFile(csv, "fraud_report.csv");
}

function downloadFile(content, filename) {
    const blob = new Blob([content], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
}

// ---------- LOAD ----------
function loadAll() {
    loadStats();
    loadTransactions();
    loadMetrics();
}

function loadStats() {
    fetch(API + "/stats")
    .then(res => res.json())
    .then(s => {
        document.getElementById("statTotal").innerText = s.total;
        document.getElementById("statFraud").innerText = s.fraud;
        document.getElementById("statLegit").innerText = s.legit;
        document.getElementById("statRate").innerText = s.fraudRate + "%";
        document.getElementById("statAmount").innerText = "Rs " + s.fraudAmount;
        document.getElementById("statUsers").innerText = s.users;

        drawPie(s.fraud, s.legit);
        drawBar(s.safe, s.lowRisk, s.suspicious, s.highRisk);
    });
}

function drawPie(fraud, legit) {
    const ctx = document.getElementById("pieChart");
    if (pieChart !== null) { pieChart.destroy(); }

    pieChart = new Chart(ctx, {
        type: "doughnut",
        data: {
            labels: ["Fraud", "Legit"],
            datasets: [{ data: [fraud, legit], backgroundColor: ["#dc2626", "#16a34a"] }]
        },
        options: { responsive: true, plugins: { legend: { position: "bottom" } } }
    });
}

function drawBar(safe, low, sus, high) {
    const ctx = document.getElementById("barChart");
    if (barChart !== null) { barChart.destroy(); }

    barChart = new Chart(ctx, {
        type: "bar",
        data: {
            labels: ["Safe", "Low Risk", "Suspicious", "High Risk"],
            datasets: [{
                label: "Transactions",
                data: [safe, low, sus, high],
                backgroundColor: ["#16a34a", "#ca8a04", "#ea580c", "#dc2626"]
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { display: false } },
            scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } }
        }
    });
}

// ---------- TABLE ----------
function loadTransactions() {
    fetch(API)
    .then(res => res.json())
    .then(list => {
        allData = list.reverse();
        renderTable();
    });
}

function renderTable() {

    const search = document.getElementById("search").value.toLowerCase();
    const filter = document.getElementById("filter").value;

    let rows = allData.filter(function (tx) {

        const user = tx.userId ? tx.userId.toLowerCase() : "";
        const loc = tx.location ? tx.location.toLowerCase() : "";
        const typ = tx.transactionType ? tx.transactionType.toLowerCase() : "";

        let matchSearch = user.indexOf(search) !== -1 ||
                          loc.indexOf(search) !== -1 ||
                          typ.indexOf(search) !== -1;

        let matchFilter;
        if (filter === "ALL") {
            matchFilter = true;
        } else if (filter === "UNLABELED") {
            matchFilter = !tx.actualLabel;
        } else {
            matchFilter = (tx.isFraud === filter);
        }

        return matchSearch && matchFilter;
    });

    if (rows.length === 0) {
        document.getElementById("list").innerHTML = '<div class="empty-msg">No transactions found</div>';
        return;
    }

    let html = "<table><tr>" +
        "<th>ID</th><th>User</th><th>Amount</th><th>Location</th>" +
        "<th>Score</th><th>Risk Level</th><th>Predicted</th>" +
        "<th>Actual</th><th>Match</th><th>Review</th></tr>";

    rows.forEach(function (tx) {

        const badge = (tx.isFraud === "FRAUD") ? "badge-fraud" : "badge-legit";

        let actualCell = "<span style='color:#94a3b8'>—</span>";
        let matchCell = "<span style='color:#94a3b8'>—</span>";

        if (tx.actualLabel) {
            const lb = (tx.actualLabel === "FRAUD") ? "lb-fraud" : "lb-legit";
            actualCell = "<span class='label-tag " + lb + "'>" + tx.actualLabel + "</span>";

            if (tx.actualLabel === tx.isFraud) {
                matchCell = "<span class='match-yes'>✔</span>";
            } else {
                matchCell = "<span class='match-no'>✘</span>";
            }
        }

        const review =
            "<button class='review-btn rv-fraud' onclick='setLabel(" + tx.id + ", \"FRAUD\")'>Fraud</button>" +
            "<button class='review-btn rv-legit' onclick='setLabel(" + tx.id + ", \"LEGIT\")'>Legit</button>" +
            "<button class='review-btn rv-clear' onclick='setLabel(" + tx.id + ", \"CLEAR\")'>✕</button>";

        html += "<tr>" +
            "<td>" + tx.id + "</td>" +
            "<td><b>" + tx.userId + "</b></td>" +
            "<td>Rs " + tx.amount + "</td>" +
            "<td>" + tx.location + "</td>" +
            "<td><b>" + tx.fraudScore + "</b></td>" +
            "<td>" + tx.riskLevel + "</td>" +
            "<td><span class='badge " + badge + "'>" + tx.isFraud + "</span></td>" +
            "<td>" + actualCell + "</td>" +
            "<td>" + matchCell + "</td>" +
            "<td>" + review + "</td>" +
            "</tr>";
    });

    document.getElementById("list").innerHTML = html + "</table>";
}

// ---------- CLEAR ----------
function clearAll() {
    if (!confirm("Delete all transactions?")) { return; }

    fetch(API, { method: "DELETE" })
    .then(() => {
        loadAll();
        document.getElementById("resultBox").className = "result-empty";
        document.getElementById("resultBox").innerText = "No transaction analyzed yet";
        document.getElementById("reasonBox").innerHTML = "";
        document.getElementById("batchSummary").innerHTML = "";
        document.getElementById("uploadStatus").innerHTML = "";
        document.getElementById("profileBox").innerHTML = "";
    });
}

// ---------- QUICK FILL ----------
function fillFraud() {
    document.getElementById("userId").value = "USER009";
    document.getElementById("amount").value = 15000;
    document.getElementById("type").value = "WITHDRAWAL";
    document.getElementById("location").value = "UNKNOWN";
    document.getElementById("time").value = "2026-01-15T03:30";
}

function fillLegit() {
    document.getElementById("userId").value = "USER008";
    document.getElementById("amount").value = 500;
    document.getElementById("type").value = "PURCHASE";
    document.getElementById("location").value = "Mumbai";
    document.getElementById("time").value = "2026-01-15T14:30";
}

// ---------- START ----------
loadAll();