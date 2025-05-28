import { useState, useEffect, useRef } from "react";
import { useForm } from "react-hook-form";
import './Assets.css';
import './AssetsCustom.css';
import AssetList from "../components/AssetList";
import AssetAddForm from "../components/AssetAddForm";
import AssetFilters from "../components/AssetFilters";
import AssetChartSection from "../components/AssetChartSection";

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
    const [searchTerm, setSearchTerm] = useState("");
    const [sortBy, setSortBy] = useState<"price" | "name" | "id">("id");
    const [sortDirection, setSortDirection] = useState<"asc" | "desc">("asc");
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(1);

    const mainContainerRef = useRef<HTMLDivElement | null>(null);
    const chartSectionRef = useRef<HTMLDivElement | null>(null);

    const fetchAssets = async () => {
        try {
            let url = `http://localhost:8000/api/v1/assets?page=${page}&size=6&`;
            if (searchTerm) url += `search=${encodeURIComponent(searchTerm)}&`;
            if (sortBy !== "id") url += `sortBy=${sortBy}&sortDirection=${sortDirection}`;
            const response = await fetch(url);
            if (!response.ok) {
                throw new Error("Nie udało się pobrać assetów");
            }
            const data = await response.json();
            setAssets(data.content || []);
            setTotalPages(data.totalPages || 1);
        } catch (err) {
            setError((err as Error).message);
        }
    };

    useEffect(() => {
        fetchAssets();
        const interval = setInterval(fetchAssets, 10000); // co 10 sekund
        return () => clearInterval(interval);
    }, [searchTerm, sortBy, sortDirection, page]); // Odświeżaj przy zmianie searchTerm, sortBy, sortDirection, page

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

            const response = await fetch("http://localhost:8000/api/v1/assets", {
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

            const response = await fetch(`http://localhost:8000/api/v1/assets/${id}`, {
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

    useEffect(() => {
        if (!selectedAsset) {
            setPriceHistory([]);
            return;
        }
        const fetchHistory = async () => {
            try {
                const response = await fetch(`http://localhost:8000/api/v1/assets/${selectedAsset.id}/history`);
                if (!response.ok) throw new Error("Nie udało się pobrać historii cen");
                const data = await response.json();
                setPriceHistory(data);
            } catch (err) {
                setPriceHistory([]);
            }
        };
        fetchHistory();
    }, [selectedAsset]);

    useEffect(() => {
        if (selectedAsset && chartSectionRef.current && mainContainerRef.current) {
            setTimeout(() => {
                mainContainerRef.current?.scrollTo({ top: 0, behavior: "smooth" });
                setTimeout(() => {
                    chartSectionRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
                }, 200);
            }, 100);
        }
    }, [selectedAsset]);

    const formatDate = (iso: string) => {
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

            <AssetFilters
                searchTerm={searchTerm}
                setSearchTerm={setSearchTerm}
                sortBy={sortBy}
                setSortBy={setSortBy}
                sortDirection={sortDirection}
                setSortDirection={setSortDirection}
                page={page}
                setPage={setPage}
                totalPages={totalPages}
            />

            {isAdmin && (
                <AssetAddForm
                    loading={loading}
                    onAdd={handleSubmit(addAssetToApi)}
                    register={register}
                />
            )}

            {error && <p className="assets-error">{error}</p>}

            <AssetList
                assets={assets}
                loading={loading}
                isAdmin={isAdmin}
                selectedAsset={selectedAsset}
                setSelectedAsset={setSelectedAsset}
                deleteAsset={deleteAsset}
            />

            {selectedAsset && (
                <AssetChartSection
                    ref={chartSectionRef}
                    selectedAsset={selectedAsset}
                    priceHistory={priceHistory}
                    formatDate={formatDate}
                />
            )}
        </div>
    );
}

export default Assets;
