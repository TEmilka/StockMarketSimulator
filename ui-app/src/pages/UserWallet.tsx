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

            const updatedWallet = await response.json();
            setAssets(updatedWallet);

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
