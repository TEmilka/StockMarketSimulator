import { useState, useEffect } from "react";
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

    return (
        <div className="container">
            <h1 className="title">Lista Aktywów</h1>

            {isAdmin && (
                <form onSubmit={handleSubmit(addAssetToApi)} className="form">
                    <input
                        {...register("symbol", { required: true })}
                        placeholder="Symbol"
                        className="input"
                    />
                    <input
                        {...register("name", { required: true })}
                        placeholder="Nazwa"
                        className="input input-email"
                    />
                    <input
                        {...register("price", { required: true })}
                        placeholder="Cena"
                        type="number"
                        className="input input-email"
                    />
                    <button
                        type="submit"
                        className="button"
                        disabled={loading}
                    >
                        {loading ? "Dodawanie..." : "Dodaj asset"}
                    </button>
                </form>
            )}

            {error && <p className="error">{error}</p>}

            <div className="asset-list">
                <h2 className="asset-list-title">Lista assetów</h2>
                {assets.length > 0 ? (
                    <ul>
                        {assets.map((asset: Asset, idx: number) => (
                            <li
                                key={idx}
                                className={`asset-item${selectedAsset?.id === asset.id ? " selected" : ""}`}
                                style={{ cursor: "pointer" }}
                                onClick={() => setSelectedAsset(asset)}
                            >
                                {asset.name} ({asset.symbol}) - {asset.price} USD
                                {isAdmin && (
                                    <button onClick={() => deleteAsset(asset.id)}>Usuń</button>
                                )}
                            </li>
                        ))}
                    </ul>
                ) : (
                    <p>Brak assetów</p>
                )}
            </div>

            {selectedAsset && (
                <div style={{ marginTop: 32 }}>
                    <h2>Wykres ceny: {selectedAsset.name} ({selectedAsset.symbol})</h2>
                    {priceHistory.length > 0 ? (
                        <Line
                            data={{
                                labels: priceHistory.map(p => p.timestamp),
                                datasets: [
                                    {
                                        label: "Cena",
                                        data: priceHistory.map(p => p.price),
                                        borderColor: "blue",
                                        backgroundColor: "rgba(0,0,255,0.1)",
                                        pointRadius: priceHistory.map((_, i) =>
                                            i === priceHistory.length - 1 ? 6 : 2
                                        ),
                                        pointBackgroundColor: priceHistory.map((_, i) =>
                                            i === priceHistory.length - 1 ? "red" : "blue"
                                        ),
                                    },
                                ],
                            }}
                            options={{
                                plugins: {
                                    legend: { display: false },
                                    tooltip: { enabled: true },
                                },
                                scales: {
                                    x: { display: true, title: { display: true, text: "Data" } },
                                    y: { display: true, title: { display: true, text: "Cena (USD)" } },
                                },
                            }}
                        />
                    ) : (
                        <p>Brak danych do wyświetlenia wykresu.</p>
                    )}
                </div>
            )}
        </div>
    );
}

export default Assets;
