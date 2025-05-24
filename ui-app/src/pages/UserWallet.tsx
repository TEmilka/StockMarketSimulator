import { useState, useEffect } from "react";

interface Asset {
    id: string;
    symbol: string;
    name: string;
    price: number;
    amount: number;
}

interface UserAccount {
    accountBalance: number;
    profit: number;
}

function UserWallet() {
    const userId = localStorage.getItem("userId"); // <-- zawsze bierze ID zalogowanego użytkownika
    const [assets, setAssets] = useState<Asset[]>([]);
    const [error, setError] = useState<string>("");
    const [loading, setLoading] = useState<boolean>(true);

    const [availableAssets, setAvailableAssets] = useState<Asset[]>([]);

    const [account, setAccount] = useState<UserAccount>({ accountBalance: 0, profit: 0 });
    const [addFundsAmount, setAddFundsAmount] = useState<string>("");

    // Nowe stany do obsługi transakcji
    const [tradeAssetId, setTradeAssetId] = useState<string>("");
    const [tradeAmount, setTradeAmount] = useState<string>("");
    const [tradeType, setTradeType] = useState<"BUY" | "SELL">("BUY");

    // Wylicz koszt/zysk transakcji na podstawie wyboru
    const selectedTradeAsset = availableAssets.find(a => a.id.toString() === tradeAssetId);
    const parsedTradeAmount = parseFloat(tradeAmount) || 0;
    const tradeValue = selectedTradeAsset ? parsedTradeAmount * selectedTradeAsset.price : 0;

    const fetchWalletDetails = async () => {
        try {
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

    const fetchAccountInfo = async () => {
        try {
            const accessToken = localStorage.getItem("accessToken");
            if (!accessToken) return;
            const response = await fetch(`http://localhost:8000/api/users/${userId}`, {
                headers: {
                    'Authorization': `Bearer ${accessToken}`,
                    'Content-Type': 'application/json'
                }
            });
            if (response.ok) {
                const data = await response.json();
                setAccount({
                    accountBalance: data.accountBalance,
                    profit: data.profit
                });
            }
        } catch (err) {
            // ignore
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
        fetchAccountInfo();
        // Odświeżaj profit i stan konta co 10 sekund
        const interval = setInterval(() => {
            fetchAccountInfo();
        }, 10000);
        return () => clearInterval(interval);
        // eslint-disable-next-line
    }, [userId]);

    const handleAddFunds = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const accessToken = localStorage.getItem("accessToken");
            if (!accessToken) throw new Error("Brak autoryzacji. Zaloguj się ponownie.");
            const response = await fetch(`http://localhost:8000/api/users/${userId}/add-funds`, {
                method: "POST",
                headers: {
                    'Authorization': `Bearer ${accessToken}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ amount: parseFloat(addFundsAmount) })
            });
            if (!response.ok) {
                const data = await response.json();
                throw new Error(data.error || "Nie udało się dodać środków");
            }
            setAddFundsAmount("");
            fetchAccountInfo();
        } catch (err) {
            setError((err as Error).message);
        }
    };

    // Nowa funkcja do kupna/sprzedaży aktywa
    const handleTrade = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const accessToken = localStorage.getItem("accessToken");
            if (!accessToken) throw new Error("Brak autoryzacji. Zaloguj się ponownie.");
            const response = await fetch(`http://localhost:8000/api/users/${userId}/wallet/trade`, {
                method: "POST",
                headers: {
                    'Authorization': `Bearer ${accessToken}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    assetId: parseInt(tradeAssetId),
                    amount: parseFloat(tradeAmount),
                    type: tradeType
                })
            });
            const data = await response.json();
            if (!response.ok) {
                throw new Error(data.error || "Nie udało się wykonać transakcji");
            }
            setTradeAssetId("");
            setTradeAmount("");
            fetchWalletDetails();
            fetchAccountInfo();
            setError(""); // Clear any previous errors
        } catch (err) {
            setError((err as Error).message);
        }
    };

    if (loading) return <p>Ładowanie portfela...</p>;
    if (error) return <p>{error}</p>;

    return (
        <div className="container">
            <h1>Portfel użytkownika {userId}</h1>
            <div>
                <strong>Stan konta:</strong> {account.accountBalance.toFixed(2)} PLN
                <br />
                <strong>Profit:</strong> {account.profit.toFixed(2)} PLN
            </div>
            <form onSubmit={handleAddFunds} style={{ margin: "16px 0" }}>
                <input
                    type="number"
                    placeholder="Kwota do dodania"
                    value={addFundsAmount}
                    onChange={e => setAddFundsAmount(e.target.value)}
                    min="0.01"
                    step="0.01"
                    required
                />
                <button type="submit">Dodaj środki</button>
            </form>
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

            <h2>Kup/Sprzedaj aktywo</h2>
            <form onSubmit={handleTrade}>
                <select
                    value={tradeAssetId}
                    onChange={e => setTradeAssetId(e.target.value)}
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
                    value={tradeAmount}
                    onChange={e => setTradeAmount(e.target.value)}
                    min="0.01"
                    step="0.01"
                    required
                />
                <select
                    value={tradeType}
                    onChange={e => setTradeType(e.target.value as "BUY" | "SELL")}
                >
                    <option value="BUY">Kup</option>
                    <option value="SELL">Sprzedaj</option>
                </select>
                <button type="submit">Wykonaj transakcję</button>
            </form>
            {/* Podgląd kosztu/zysku transakcji */}
            {selectedTradeAsset && parsedTradeAmount > 0 && (
                <div style={{ marginTop: 8, marginBottom: 16 }}>
                    {tradeType === "BUY" ? (
                        <span>
                            Koszt zakupu: <strong>{tradeValue.toFixed(2)} USD</strong>
                        </span>
                    ) : (
                        <span>
                            Otrzymasz za sprzedaż: <strong>{tradeValue.toFixed(2)} USD</strong>
                        </span>
                    )}
                </div>
            )}
        </div>
    );
}

export default UserWallet;
