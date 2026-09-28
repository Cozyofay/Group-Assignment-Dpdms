/* Leaflet map of approved incidents. Markers are coloured by hazard and clickable for details. */
(function () {
    const container = document.getElementById('map');
    if (!container || typeof L === 'undefined') {
        const fallback = document.getElementById('map-fallback');
        if (fallback) {
            fallback.textContent = 'The map library could not be loaded (no internet connection?). '
                + 'Incident coordinates are still available in the tables and reports.';
        }
        return;
    }

    const colours = {
        FLOOD: '#1f6feb', DROUGHT: '#b8860b', FIRE: '#d1242f',
        ZOONOTIC_DISEASE: '#8250df', MINING_ACCIDENT: '#57606a'
    };

    // Rushinga district, Mashonaland Central
    const map = L.map('map').setView([-16.60, 32.10], 9);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 18,
        attribution: '&copy; OpenStreetMap contributors'
    }).addTo(map);

    fetch((window.DPDMS_MAP_URL || '/map-data') + (window.DPDMS_MAP_QUERY || ''))
        .then(response => response.json())
        .then(points => {
            if (!points.length) {
                document.getElementById('map-fallback').textContent =
                    'No approved incidents to plot yet.';
                return;
            }
            const markers = [];
            points.forEach(point => {
                const marker = L.circleMarker([point.latitude, point.longitude], {
                    radius: 9,
                    color: colours[point.hazard] || '#333',
                    fillColor: colours[point.hazard] || '#333',
                    fillOpacity: 0.75,
                    weight: 2
                }).addTo(map);

                let indicators = '';
                if (point.indicators) {
                    indicators = Object.entries(point.indicators)
                        .map(([key, value]) => '<li><b>' + key.replace(/([A-Z])/g, ' $1').toLowerCase()
                            + ':</b> ' + value + '</li>')
                        .join('');
                }
                marker.bindPopup(
                    '<b>' + point.hazardLabel + ' #' + point.id + '</b><br>' +
                    point.ward + ', ' + point.district + '<br>' +
                    '<i>' + (point.occurredAt || '').replace('T', ' ') + '</i><br>' +
                    'Severity: ' + point.severity + '<br>' +
                    point.headline + '<ul>' + indicators + '</ul>' +
                    '<small>Reported by ' + point.reporterUsername + '</small>');
                markers.push(marker);
            });
            map.fitBounds(L.featureGroup(markers).getBounds().pad(0.2));
        })
        .catch(() => {
            document.getElementById('map-fallback').textContent =
                'Could not load map data. Is the dashboard-service running?';
        });
})();
