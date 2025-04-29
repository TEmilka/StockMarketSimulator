import { useState, useEffect } from "react";
import { useParams } from "react-router-dom";

interface Asset {
    id: string;
    symbol: string;
    name: string;
    price: number;
    amount: number;
}

function UserWallet() {
    const {userId} = useParams<{ userId: string }>();
    const [assets, setAssets] = useState<Asset[]>([]);
    const [error, setError] = useState<string>("");
    const [loading, setLoading] = useState<boolean>(true);

    const [newAssetId, setNewAssetId] = useState<string>("");
    const [newAssetAmount, setNewAssetAmount] = useState<string>("");
    const [availableAssets, setAvailableAssets] = useState<Asset[]>([]);

    const fetchWalletDetails = async () => {
        try {
            setLoading(true);
            const accessToken = localStorage.getItem("accessToken");
            if (!accessToken) {
                throw new Error("Brak autoryzacji. Zaloguj się ponownie.");
            }

            const response = await fetch(`http://localhost:8000/api/users/${userId}/wallet/details`, {
                headers: {
                    'Authorization': `Bearer ${accessToken}`,
                    'Content-Type': 'application/json'
                }
            });

            if (response.status === 401) {
                throw new Error("Sesja wygasła. Zaloguj się ponownie.");
            }
            if (response.status === 403) {
                throw new Error("Brak uprawnień do wyświetlenia tego portfela.");
            }
            if (!response.ok) {
                throw new Error("Nie udało się pobrać portfela");
            }

            const data = await response.json();
            setAssets(data);
        } catch (err) {
            setError((err as Error).message);
        } finally {
            setLoading(false);
        }
    };

    const fetchAvailableAssets = async () => {
        try {
            const response = await fetch("http://localhost:8000/api/assets");
            if (!response.ok) {
                throw new Error("Nie udało się pobrać dostępnych aktywów");
            }
            const data = await response.json();
            setAvailableAssets(data);
        } catch (err) {
            console.error("Error fetching available assets:", err);
        }
    };

    useEffect(() => {
        fetchWalletDetails();
        fetchAvailableAssets();
    }, [userId]);

    const addAssetToWallet = async (assetId: string, amount: string) => {
        try {
            const accessToken = localStorage.getItem("accessToken");
            if (!accessToken) {
                throw new Error("Brak autoryzacji. Zaloguj się ponownie.");
            }

            const response = await fetch(`http://localhost:8000/api/users/${userId}/wallet/add`, {
                method: "POST",
                headers: { 
                    "Content-Type": "application/json",
                    'Authorization': `Bearer ${accessToken}`
                },
                body: JSON.stringify({
                    assetId: parseInt(assetId),
                    amount: parseFloat(amount)
                }),
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.error || "Nie udało się dodać aktywa");
            }

            const updatedWallet = await response.json();
            setAssets(updatedWallet);
            setNewAssetId("");
            setNewAssetAmount("");
            setError(""); // Clear any previous errors
        } catch (err) {
            setError((err as Error).message);
            console.error("Error adding asset:", err);
        }
    };


    if (loading) return <p>Ładowanie portfela...</p>;
    if (error) return <p>{error}</p>;

    return (
        <div className="container">
            <h1>Portfel użytkownika {userId}</h1>
            <h2>Assety użytkownika</h2>
            {assets.length > 0 ? (
                <ul>
                    {assets.map((asset) => (
                        <li key={asset.id}>
                            <strong>{asset.name}</strong> ({asset.symbol}) - {asset.amount} szt. @ {asset.price} USD
                        </li>
                    ))}
                </ul>
            ) : (
                <p>Brak aktywów w portfelu.</p>
            )}

            <h2>Dodaj aktywo</h2>
            <form
                onSubmit={(e) => {
                    e.preventDefault();
                    addAssetToWallet(newAssetId, newAssetAmount);
                }}
            >
                <select
                    value={newAssetId}
                    onChange={(e) => setNewAssetId(e.target.value)}
                    required
                >
                    <option value="">Wybierz aktywo</option>
                    {availableAssets.map((asset) => (
                        <option key={asset.id} value={asset.id}>
                            {asset.name} ({asset.symbol}) - {asset.price} USD
                        </option>
                    ))}
                </select>
                <input
                    type="number"
                    placeholder="Ilość"
                    value={newAssetAmount}
                    onChange={(e) => setNewAssetAmount(e.target.value)}
                    required
                    min="0.01"
                    step="0.01"
                />
                <button type="submit">Dodaj</button>
            </form>
        </div>
    );
}

export default UserWallet;
