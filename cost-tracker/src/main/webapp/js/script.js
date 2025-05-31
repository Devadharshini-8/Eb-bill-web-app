document.addEventListener('DOMContentLoaded', function () {
    if (window.location.pathname.endsWith('tracking.html')) {
        fetchUsageData();
        setInterval(fetchUsageData, 60000); // Refresh every minute
    } else if (window.location.pathname.endsWith('payment.html')) {
        fetchPaymentData();
        setInterval(fetchPaymentData, 60000); // Refresh every minute
    } else if (window.location.pathname.endsWith('notification.html')) {
        fetchNotificationData();
        setInterval(fetchNotificationData, 60000); // Refresh every minute
    } else if (window.location.pathname.endsWith('fine_payment.html')) {
        fetchFineData();
        setInterval(fetchFineData, 60000); // Refresh every minute
    }
});

function fetchUsageData() {
    fetch('usage')
        .then(response => response.json())
        .then(data => {
            const usageBody = document.getElementById('usageBody');
            usageBody.innerHTML = '';
            data.forEach(usage => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td>${new Date(usage.timestamp).toLocaleString()}</td>
                    <td>${usage.hours}</td>
                    <td>₹${usage.cost.toFixed(2)}</td>
                `;
                usageBody.appendChild(row);
            });
        })
        .catch(error => {
            console.error('Error fetching usage data:', error);
            document.getElementById('error').textContent = 'Failed to load usage data.';
        });
}

function fetchPaymentData() {
    fetch('payment')
        .then(response => response.json())
        .then(data => {
            document.getElementById('totalCost').textContent = data.totalCost.toFixed(2);
        })
        .catch(error => {
            console.error('Error fetching payment data:', error);
            document.getElementById('error').textContent = 'Failed to load payment data.';
        });
}

function fetchNotificationData() {
    fetch('notifications')
        .then(response => response.json())
        .then(data => {
            const notificationBody = document.getElementById('notificationBody');
            notificationBody.innerHTML = '';
            data.forEach(notification => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td>${notification.message}</td>
                    <td>${new Date(notification.createdAt).toLocaleString()}</td>
                `;
                notificationBody.appendChild(row);
            });
        })
        .catch(error => {
            console.error('Error fetching notifications:', error);
            document.getElementById('error').textContent = 'Failed to load notifications.';
        });
}

function fetchFineData() {
    fetch('fine')
        .then(response => response.json())
        .then(data => {
            const fineDetails = document.getElementById('fineDetails');
            if (data.fineId) {
                fineDetails.innerHTML = `
                    <h2 class="text-white">Fine Details</h2>
                    <p>Fine Amount: ₹${data.fineAmount.toFixed(2)}</p>
                    <p>Due Date: ${data.dueDate ? new Date(data.dueDate).toLocaleDateString() : 'N/A'}</p>
                    <p>Status: ${data.isPaid ? 'Paid' : 'Unpaid'}</p>
                    ${!data.isPaid ? '<form action="fine" method="post"><button type="submit">Pay Fine</button></form>' : ''}
                `;
            } else {
                fineDetails.innerHTML = '<p class="text-white">No outstanding fines.</p>';
            }
        })
        .catch(error => {
            console.error('Error fetching fine details:', error);
            document.getElementById('error').textContent = 'Failed to load fine details.';
        });
}