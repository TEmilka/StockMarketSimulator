import { useState, useEffect } from "react";
import { useParams } from "react-router-dom";

interface Asset {
    id: string;
    symbol: string;
    name: string;
    price: number;
    amount: number; // Ilość posiadana w portfelu
}

function UserWallet() {
    const { userId } = useParams<{ userId: string }>();
    const [assets, setAssets] = useState<Asset[]>([]);
    const [error, setError] = useState<string>("");
    const [loading, setLoading] = useState<boolean>(true);

    const [newAssetId, setNewAssetId] = useState<string>("");
    const [newAssetAmount, setNewAssetAmount] = useState<string>("");

    // Funkcja do pobrania portfela użytkownika
    const fetchWalletDetails = async () => {
        try {
            setLoading(true);
            const response = await fetch(`http://localhost:8000/api/users/${userId}/wallet/details`);
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

    // Pobieramy portfel przy pierwszym renderze
    useEffect(() => {
        fetchWalletDetails();
    }, [userId]);

    const addAssetToWallet = async (assetId: string, amount: string) => {
        try {
            const response = await fetch(`http://localhost:8000/api/users/${userId}/wallet/add`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ assetId: Number(assetId), amount: Number(amount) }),
            });

            if (!response.ok) {
                throw new Error("Nie udało się dodać aktywa");
            }

            const updatedWallet = await response.json(); // Otrzymujemy nową listę aktywów
            setAssets(updatedWallet); // Ustawiamy nowy stan bez odświeżania

            setNewAssetId("");
            setNewAssetAmount("");
        } catch (err) {
            setError((err as Error).message);
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

            {/* Formularz dodawania aktywów */}
            <h2>Dodaj aktywo</h2>
            <form
                onSubmit={(e) => {
                    e.preventDefault();
                    addAssetToWallet(newAssetId, newAssetAmount);
                }}
            >
                <input
                    type="text"
                    placeholder="ID aktywa"
                    value={newAssetId}
                    onChange={(e) => setNewAssetId(e.target.value)}
                    required
                />
                <input
                    type="number"
                    placeholder="Ilość"
                    value={newAssetAmount}
                    onChange={(e) => setNewAssetAmount(e.target.value)}
                    required
                />
                <button type="submit">Dodaj</button>
            </form>
        </div>
    );
}

export default UserWallet;
