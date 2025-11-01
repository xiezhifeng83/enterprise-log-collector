// API Base URL
const API_BASE = '/api/v1';

// Pagination state
let currentPage = 0;
let totalPages = 1;
let currentTxPage = 0;
let totalTxPages = 1;

// Initialize default time range (last 24 hours)
window.addEventListener('DOMContentLoaded', () => {
    const now = new Date();
    const yesterday = new Date(now.getTime() - 24 * 60 * 60 * 1000);

    document.getElementById('startTime').value = formatDateTimeLocal(yesterday);
    document.getElementById('endTime').value = formatDateTimeLocal(now);
    document.getElementById('txStartTime').value = formatDateTimeLocal(yesterday);
    document.getElementById('txEndTime').value = formatDateTimeLocal(now);
    document.getElementById('statsStartTime').value = formatDateTimeLocal(yesterday);
    document.getElementById('statsEndTime').value = formatDateTimeLocal(now);
});

// Tab switching
function showTab(tabName) {
    // Hide all tabs
    document.querySelectorAll('.tab-content').forEach(tab => {
        tab.classList.remove('active');
    });
    document.querySelectorAll('.tab-button').forEach(btn => {
        btn.classList.remove('active');
    });

    // Show selected tab
    document.getElementById(`${tabName}-tab`).classList.add('active');
    event.target.classList.add('active');
}

// Search logs
async function searchLogs(page = 0) {
    const serverId = document.getElementById('serverId').value;
    const startTime = document.getElementById('startTime').value;
    const endTime = document.getElementById('endTime').value;
    const errorOnly = document.getElementById('errorOnly').checked;

    let url = `${API_BASE}/logs?page=${page}&size=20`;

    if (serverId) url += `&serverId=${serverId}`;
    if (startTime) url += `&startTime=${encodeURIComponent(startTime)}`;
    if (endTime) url += `&endTime=${encodeURIComponent(endTime)}`;
    if (errorOnly) url += `&errorOnly=true`;

    try {
        const response = await fetch(url);
        if (!response.ok) throw new Error('查询失败');

        const data = await response.json();
        displayLogs(data);
        updatePagination(data);
    } catch (error) {
        console.error('Error:', error);
        alert('查询日志失败: ' + error.message);
    }
}

// Display logs
function displayLogs(data) {
    const tbody = document.getElementById('logs-tbody');

    if (!data.content || data.content.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="no-data">没有找到匹配的日志</td></tr>';
        return;
    }

    tbody.innerHTML = data.content.map(log => `
        <tr class="${log.errorIndicator ? 'error-row' : ''}">
            <td>${log.id}</td>
            <td>${log.serverId}</td>
            <td>${log.logType}</td>
            <td>${log.fileName}</td>
            <td>${formatDateTime(log.originalTimestamp)}</td>
            <td style="max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                ${escapeHtml(log.content)}
            </td>
            <td>${log.transactionId || '-'}</td>
            <td>
                ${log.errorIndicator ? '<span class="badge error">错误</span>' : '<span class="badge success">正常</span>'}
                ${log.parsed ? '<span class="badge info">已解析</span>' : ''}
            </td>
            <td>
                <button class="btn action-btn btn-primary" onclick="viewLogDetail(${log.id})">详情</button>
                ${log.transactionId ? `<button class="btn action-btn btn-secondary" onclick="viewTransaction('${log.transactionId}')">事务</button>` : ''}
            </td>
        </tr>
    `).join('');
}

// Update pagination
function updatePagination(data) {
    currentPage = data.number;
    totalPages = data.totalPages;

    document.getElementById('total-logs').textContent = `总记录: ${data.totalElements}`;
    document.getElementById('page-info').textContent = `第 ${currentPage + 1} 页 / 共 ${totalPages} 页`;

    document.getElementById('prev-btn').disabled = data.first;
    document.getElementById('next-btn').disabled = data.last;
}

// Pagination controls
function previousPage() {
    if (currentPage > 0) {
        searchLogs(currentPage - 1);
    }
}

function nextPage() {
    if (currentPage < totalPages - 1) {
        searchLogs(currentPage + 1);
    }
}

// Reset filters
function resetFilters() {
    document.getElementById('serverId').value = '';
    document.getElementById('errorOnly').checked = false;

    const now = new Date();
    const yesterday = new Date(now.getTime() - 24 * 60 * 60 * 1000);
    document.getElementById('startTime').value = formatDateTimeLocal(yesterday);
    document.getElementById('endTime').value = formatDateTimeLocal(now);

    searchLogs();
}

// View log detail
async function viewLogDetail(logId) {
    try {
        const response = await fetch(`${API_BASE}/logs/${logId}`);
        if (!response.ok) throw new Error('获取日志详情失败');

        const log = await response.json();

        const modalBody = document.getElementById('modal-body');
        modalBody.innerHTML = `
            <div class="detail-row"><strong>日志ID:</strong> ${log.id}</div>
            <div class="detail-row"><strong>服务器ID:</strong> ${log.serverId}</div>
            <div class="detail-row"><strong>日志类型:</strong> ${log.logType}</div>
            <div class="detail-row"><strong>文件名:</strong> ${log.fileName}</div>
            <div class="detail-row"><strong>文件路径:</strong> ${log.logPath}</div>
            <div class="detail-row"><strong>原始时间:</strong> ${formatDateTime(log.originalTimestamp)}</div>
            <div class="detail-row"><strong>收集时间:</strong> ${formatDateTime(log.collectionTimestamp)}</div>
            <div class="detail-row"><strong>事务ID:</strong> ${log.transactionId || '无'}</div>
            <div class="detail-row"><strong>状态:</strong>
                ${log.errorIndicator ? '<span class="badge error">错误</span>' : '<span class="badge success">正常</span>'}
                ${log.parsed ? '<span class="badge info">已解析</span>' : '<span class="badge warning">未解析</span>'}
            </div>
            <div class="detail-row">
                <strong>日志内容:</strong>
                <div class="log-content">${escapeHtml(log.content)}</div>
            </div>
        `;

        document.getElementById('logModal').style.display = 'block';
    } catch (error) {
        console.error('Error:', error);
        alert('获取日志详情失败: ' + error.message);
    }
}

// Close modal
function closeModal() {
    document.getElementById('logModal').style.display = 'none';
}

// Search transactions
async function searchTransactions(page = 0) {
    const status = document.getElementById('txStatus').value;
    const startTime = document.getElementById('txStartTime').value;
    const endTime = document.getElementById('txEndTime').value;

    let url = `${API_BASE}/transactions?page=${page}&size=20`;

    if (status) url += `&status=${status}`;
    if (startTime) url += `&startTime=${encodeURIComponent(startTime)}`;
    if (endTime) url += `&endTime=${encodeURIComponent(endTime)}`;

    try {
        const response = await fetch(url);
        if (!response.ok) throw new Error('查询失败');

        const data = await response.json();
        displayTransactions(data);
        updateTxPagination(data);
    } catch (error) {
        console.error('Error:', error);
        alert('查询事务失败: ' + error.message);
    }
}

// Display transactions
function displayTransactions(data) {
    const tbody = document.getElementById('transactions-tbody');

    if (!data.content || data.content.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="no-data">没有找到匹配的事务</td></tr>';
        return;
    }

    tbody.innerHTML = data.content.map(tx => `
        <tr>
            <td>${tx.transactionId}</td>
            <td>${getStatusBadge(tx.status)}</td>
            <td>${tx.sourceSystem || '-'}</td>
            <td>${tx.targetSystem || '-'}</td>
            <td>${formatDateTime(tx.startTime)}</td>
            <td>${tx.endTime ? formatDateTime(tx.endTime) : '-'}</td>
            <td>${tx.durationMs || '-'}</td>
            <td>${tx.retryCount}</td>
            <td>
                <button class="btn action-btn btn-primary" onclick="viewTransactionLogs('${tx.transactionId}')">查看日志</button>
            </td>
        </tr>
    `).join('');
}

// Update transaction pagination
function updateTxPagination(data) {
    currentTxPage = data.number;
    totalTxPages = data.totalPages;

    document.getElementById('total-transactions').textContent = `总记录: ${data.totalElements}`;
    document.getElementById('tx-page-info').textContent = `第 ${currentTxPage + 1} 页 / 共 ${totalTxPages} 页`;

    document.getElementById('prev-tx-btn').disabled = data.first;
    document.getElementById('next-tx-btn').disabled = data.last;
}

// Transaction pagination controls
function previousTxPage() {
    if (currentTxPage > 0) {
        searchTransactions(currentTxPage - 1);
    }
}

function nextTxPage() {
    if (currentTxPage < totalTxPages - 1) {
        searchTransactions(currentTxPage + 1);
    }
}

// Reset transaction filters
function resetTxFilters() {
    document.getElementById('txStatus').value = '';

    const now = new Date();
    const yesterday = new Date(now.getTime() - 24 * 60 * 60 * 1000);
    document.getElementById('txStartTime').value = formatDateTimeLocal(yesterday);
    document.getElementById('txEndTime').value = formatDateTimeLocal(now);

    searchTransactions();
}

// View transaction logs
async function viewTransactionLogs(transactionId) {
    try {
        const response = await fetch(`${API_BASE}/transactions/${transactionId}/logs`);
        if (!response.ok) throw new Error('获取事务日志失败');

        const logs = await response.json();

        const modalBody = document.getElementById('modal-body');
        modalBody.innerHTML = `
            <h3>事务 ${transactionId} 的日志记录</h3>
            <div style="margin-top: 20px;">
                ${logs.map((log, index) => `
                    <div style="margin-bottom: 20px; padding: 15px; background: #f9f9f9; border-radius: 4px;">
                        <div style="margin-bottom: 10px;">
                            <strong>步骤 ${index + 1}</strong> -
                            <span style="color: #667eea;">${formatDateTime(log.originalTimestamp)}</span> -
                            <strong>${log.logType}</strong>
                            ${log.errorIndicator ? '<span class="badge error">错误</span>' : ''}
                        </div>
                        <div class="log-content">${escapeHtml(log.content)}</div>
                    </div>
                `).join('')}
            </div>
        `;

        document.getElementById('logModal').style.display = 'block';
    } catch (error) {
        console.error('Error:', error);
        alert('获取事务日志失败: ' + error.message);
    }
}

// View transaction from log
function viewTransaction(transactionId) {
    showTab('transactions');
    document.querySelector('[onclick="showTab(\'transactions\')"]').click();
    setTimeout(() => viewTransactionLogs(transactionId), 100);
}

// Load statistics
async function loadStatistics() {
    const startTime = document.getElementById('statsStartTime').value;
    const endTime = document.getElementById('statsEndTime').value;

    if (!startTime || !endTime) {
        alert('请选择时间范围');
        return;
    }

    let url = `${API_BASE}/logs/statistics?startTime=${encodeURIComponent(startTime)}&endTime=${encodeURIComponent(endTime)}`;

    try {
        const response = await fetch(url);
        if (!response.ok) throw new Error('获取统计失败');

        const stats = await response.json();
        displayStatistics(stats);
    } catch (error) {
        console.error('Error:', error);
        alert('获取统计数据失败: ' + error.message);
    }
}

// Display statistics
function displayStatistics(stats) {
    document.getElementById('stat-total').textContent = stats.totalLogs.toLocaleString();
    document.getElementById('stat-errors').textContent = stats.errorLogs.toLocaleString();
    document.getElementById('stat-parsed').textContent = stats.parsedLogs.toLocaleString();
    document.getElementById('stat-unparsed').textContent = stats.unparsedLogs.toLocaleString();
    document.getElementById('stat-error-rate').textContent = (stats.errorRate * 100).toFixed(2) + '%';
}

// Utility functions
function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return '-';
    return dateTimeStr.replace('T', ' ');
}

function formatDateTimeLocal(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function getStatusBadge(status) {
    const badges = {
        'SUCCESS': '<span class="badge success">成功</span>',
        'FAILED': '<span class="badge error">失败</span>',
        'TIMEOUT': '<span class="badge warning">超时</span>',
        'IN_PROGRESS': '<span class="badge info">进行中</span>'
    };
    return badges[status] || status;
}

// Close modal when clicking outside
window.onclick = function(event) {
    const modal = document.getElementById('logModal');
    if (event.target === modal) {
        closeModal();
    }
}
