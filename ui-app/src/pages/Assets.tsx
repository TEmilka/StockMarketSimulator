import { useState, useEffect, useRef } from "react";
import { useForm } from "react-hook-form";
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
import './Assets.css';
import './AssetsCustom.css';

ChartJS.register(LineElement, PointElement, LinearScale, CategoryScale, Tooltip, Legend);

interface AssetForm {
    symbol: string;
    name: string;
    price: number;
}

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

function Assets() {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [assets, setAssets] = useState<Asset[]>([]);
    const { register, handleSubmit, reset } = useForm<AssetForm>();
    const [isAdmin, setIsAdmin] = useState(false);
    const [selectedAsset, setSelectedAsset] = useState<Asset | null>(null);
    const [priceHistory, setPriceHistory] = useState<PriceHistoryPoint[]>([]);

    // Ref do całego kontenera
    const mainContainerRef = useRef<HTMLDivElement | null>(null);
    const chartSectionRef = useRef<HTMLDivElement | null>(null);

    const fetchAssets = async () => {
        try {
            const response = await fetch("http://localhost:8000/api/assets");
            if (!response.ok) {
                throw new Error("Nie udało się pobrać assetów");
            }
            const data = await response.json();
            setAssets(data);
        } catch (err) {
            setError((err as Error).message);
        }
    };

    useEffect(() => {
        fetchAssets();
        const interval = setInterval(fetchAssets, 10000); // co 10 sekund
        return () => clearInterval(interval);
    }, []);

    useEffect(() => {
        const checkAdminStatus = () => {
            const token = localStorage.getItem("accessToken");
            if (token) {
                try {
                    const payload = JSON.parse(atob(token.split(".")[1]));
                    setIsAdmin(payload.authorities === "ROLE_ADMIN");
                } catch (err) {
                    console.error("Error parsing token:", err);
                    setIsAdmin(false);
                }
            }
        };
        checkAdminStatus();
    }, []);

    const addAssetToApi = async (data: AssetForm) => {
        setLoading(true);
        setError("");
        try {
            const accessToken = localStorage.getItem("accessToken");
            if (!accessToken) {
                throw new Error("Brak autoryzacji. Zaloguj się ponownie.");
            }

            const response = await fetch("http://localhost:8000/api/assets", {
                method: "POST",
                headers: { 
                    "Content-Type": "application/json",
                    'Authorization': `Bearer ${accessToken}`
                },
                body: JSON.stringify(data),
            });

            if (response.status === 401) {
                throw new Error("Sesja wygasła. Zaloguj się ponownie.");
            }
            if (response.status === 403) {
                throw new Error("Brak uprawnień do dodawania aktywów.");
            }
            if (!response.ok) {
                throw new Error("Nie udało się dodać assetu");
            }

            reset();
            await fetchAssets();
        } catch (err) {
            setError((err as Error).message);
        } finally {
            setLoading(false);
        }
    };

    const deleteAsset = async (id: number) => {
        try {
            const accessToken = localStorage.getItem("accessToken");
            if (!accessToken) {
                throw new Error("Brak autoryzacji. Zaloguj się ponownie.");
            }

            const response = await fetch(`http://localhost:8000/api/assets/${id}`, {
                method: "DELETE",
                headers: {
                    'Authorization': `Bearer ${accessToken}`
                }
            });

            if (response.status === 401) {
                throw new Error("Sesja wygasła. Zaloguj się ponownie.");
            }
            if (response.status === 403) {
                throw new Error("Brak uprawnień to usuwania aktywów.");
            }
            if (!response.ok) {
                throw new Error("Nie udało się usunąć assetu");
            }

            setAssets(assets.filter(asset => asset.id !== id));
        } catch (err) {
            setError((err as Error).message);
        }
    };

    // Pobierz historię cen po wyborze aktywa
    useEffect(() => {
        if (!selectedAsset) {
            setPriceHistory([]);
            return;
        }
        const fetchHistory = async () => {
            try {
                const response = await fetch(`http://localhost:8000/api/assets/${selectedAsset.id}/history`);
                if (!response.ok) throw new Error("Nie udało się pobrać historii cen");
                const data = await response.json();
                setPriceHistory(data);
            } catch (err) {
                setPriceHistory([]);
            }
        };
        fetchHistory();
    }, [selectedAsset]);

    // Scrolluj do wykresu po wyborze aktywa
    useEffect(() => {
        if (selectedAsset && chartSectionRef.current && mainContainerRef.current) {
            setTimeout(() => {
                // Najpierw scrollujemy do góry kontenera
                mainContainerRef.current?.scrollTo({ top: 0, behavior: "smooth" });
                // Potem do wykresu (po chwili)
                setTimeout(() => {
                    chartSectionRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
                }, 200);
            }, 100);
        }
    }, [selectedAsset]);

    // Funkcja do formatowania daty (np. "2024-05-25" lub "25.05.2024")
    const formatDate = (iso: string) => {
        // Jeśli timestamp zawiera godzinę, bierz tylko datę
        const date = iso.split("T")[0];
        const [year, month, day] = date.split("-");
        return `${day}.${month}.${year}`;
    };

    return (
        <div className="assets-main-container" ref={mainContainerRef} style={{ overflowY: "auto", maxHeight: "100vh" }}>
            <div className="assets-header">
                <h1 className="assets-title">Aktywa giełdowe</h1>
                <p className="assets-desc">
                    Tutaj znajdziesz wszystkie dostępne aktywa giełdowe. Kliknij w wybrane aktywo, aby zobaczyć jego wykres cenowy oraz szczegóły.<br />
                    <span className="assets-hint">Ceny aktualizują się automatycznie co 10 sekund.</span>
                </p>
            </div>

            {isAdmin && (
                <form onSubmit={handleSubmit(addAssetToApi)} className="assets-form">
                    <input
                        {...register("symbol", { required: true })}
                        placeholder="Symbol"
                        className="assets-input"
                    />
                    <input
                        {...register("name", { required: true })}
                        placeholder="Nazwa"
                        className="assets-input"
                    />
                    <input
                        {...register("price", { required: true })}
                        placeholder="Cena"
                        type="number"
                        className="assets-input"
                    />
                    <button
                        type="submit"
                        className="assets-btn"
                        disabled={loading}
                    >
                        {loading ? "Dodawanie..." : "Dodaj asset"}
                    </button>
                </form>
            )}

            {error && <p className="assets-error">{error}</p>}

            <div className="assets-list-section">
                <h2 className="assets-list-title">Lista aktywów</h2>
                <div className="assets-list-info">
                    <span>Wybierz aktywo, aby zobaczyć szczegóły i wykres cenowy.</span>
                </div>
                <div className="assets-list">
                    {assets.length > 0 ? (
                        assets.map((asset: Asset) => (
                            <div
                                key={asset.id}
                                className={`assets-list-item${selectedAsset?.id === asset.id ? " selected" : ""}`}
                                onClick={() => setSelectedAsset(asset)}
                            >
                                <div className="assets-list-main">
                                    <span className="assets-list-symbol">{asset.symbol}</span>
                                    <span className="assets-list-name">{asset.name}</span>
                                </div>
                                <div className="assets-list-details">
                                    <span className="assets-list-price">{asset.price.toFixed(2)} USD</span>
                                    {isAdmin && (
                                        <button className="assets-delete-btn" onClick={e => { e.stopPropagation(); deleteAsset(asset.id); }}>
                                            Usuń
                                        </button>
                                    )}
                                </div>
                            </div>
                        ))
                    ) : (
                        <p className="assets-list-empty">Brak aktywów</p>
                    )}
                </div>
            </div>

            {selectedAsset && (
                <div className="assets-chart-section" ref={chartSectionRef}>
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
                                            borderColor: "#2563eb",
                                            backgroundColor: "rgba(37,99,235,0.08)",
                                            pointRadius: priceHistory.map((_, i) =>
                                                i === priceHistory.length - 1 ? 7 : 3
                                            ),
                                            pointBackgroundColor: priceHistory.map((_, i) =>
                                                i === priceHistory.length - 1 ? "#10b981" : "#2563eb"
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
                                                callback: function(value, index, values) {
                                                    // Skróć datę na osi X
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
            )}
        </div>
    );
}

export default Assets;
