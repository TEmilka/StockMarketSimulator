import { forwardRef } from "react";
import { Line } from "react-chartjs-2";
import {
    Chart as ChartJS,
    LineElement,
    PointElement,
    LinearScale,
    CategoryScale,
    Tooltip,
    Legend,
} from "chart.js";

ChartJS.register(LineElement, PointElement, LinearScale, CategoryScale, Tooltip, Legend);

interface Asset {
    id: number;
    symbol: string;
    name: string;
    price: number;
}

interface PriceHistoryPoint {
    timestamp: string;
    price: number;
}

interface Props {
    selectedAsset: Asset;
    priceHistory: PriceHistoryPoint[];
    formatDate: (iso: string) => string;
}

const AssetChartSection = forwardRef<HTMLDivElement, Props>(
    ({ selectedAsset, priceHistory, formatDate }, ref) => (
        <div className="assets-chart-section" ref={ref}>
            <h2 className="assets-chart-title">
                Wykres ceny: {selectedAsset.name} ({selectedAsset.symbol})
            </h2>
            <div className="assets-chart-info">
                <span>
                    Wykres przedstawia historię cen wybranego aktywa. Ostatni punkt to aktualna cena rynkowa.
                </span>
            </div>
            <div className="assets-chart-container">
                {priceHistory.length > 0 ? (
                    <Line
                        data={{
                            labels: priceHistory.map(p => formatDate(p.timestamp)),
                            datasets: [
                                {
                                    label: "Cena",
                                    data: priceHistory.map(p => p.price),
                                    borderColor: "#ff3c5f",
                                    backgroundColor: "rgba(255,60,95,0.08)",
                                    pointRadius: priceHistory.map((_, i) =>
                                        i === priceHistory.length - 1 ? 7 : 3
                                    ),
                                    pointBackgroundColor: priceHistory.map((_, i) =>
                                        i === priceHistory.length - 1 ? "#ef4444" : "#9340ff"
                                    ),
                                    tension: 0.3,
                                },
                            ],
                        }}
                        options={{
                            plugins: {
                                legend: { display: false },
                                tooltip: { enabled: true },
                            },
                            scales: {
                                x: {
                                    display: true,
                                    title: { display: true, text: "Data" },
                                    ticks: {
                                        callback: function(value) {
                                            // @ts-ignore
                                            const label = this.getLabelForValue(value);
                                            return label;
                                        }
                                    }
                                },
                                y: { display: true, title: { display: true, text: "Cena (USD)" } },
                            },
                        }}
                    />
                ) : (
                    <p className="assets-chart-empty">Brak danych do wyświetlenia wykresu.</p>
                )}
            </div>
        </div>
    )
);

export default AssetChartSection;
