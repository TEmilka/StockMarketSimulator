import { useState, useEffect, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import "./UserWalletCustom.css";

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
    username?: string;
}

function UserWallet() {
    const navigate = useNavigate();
    const params = useParams<{ userId?: string }>();
    const userId = params.userId ?? localStorage.getItem("userId");

    const [assets, setAssets] = useState<Asset[]>([]);
    const [error, setError] = useState<string>("");
    const [loading, setLoading] = useState<boolean>(true);

    const [availableAssets, setAvailableAssets] = useState<Asset[]>([]);

    const [account, setAccount] = useState<UserAccount>({ accountBalance: 0, profit: 0, username: "" });
    const [addFundsAmount, setAddFundsAmount] = useState<string>("");

    const [tradeAssetId, setTradeAssetId] = useState<string>("");
    const [tradeAmount, setTradeAmount] = useState<string>("");
    const [tradeType, setTradeType] = useState<"BUY" | "SELL">("BUY");

    const selectedTradeAsset = availableAssets.find(a => a.id.toString() === tradeAssetId);
    const parsedTradeAmount = parseFloat(tradeAmount) || 0;
    const tradeValue = selectedTradeAsset ? parsedTradeAmount * selectedTradeAsset.price : 0;

    const walletLayoutRef = useRef<HTMLDivElement | null>(null);

    const fetchWalletDetails = async () => {
        try {
            const response = await fetch(`http://localhost:8000/api/v1/users/${userId}/wallet/details`, {
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                let errorMsg = 'Failed to fetch wallet details';
                try {
                    const errorData = await response.json();
                    errorMsg = errorData.error || errorMsg;
                } catch {
                    // response is not JSON (probably HTML)
                }
                if (response.status === 401) {
                    navigate('/login');
                    return;
                }
                throw new Error(errorMsg);
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
            const response = await fetch(`http://localhost:8000/api/v1/users/${userId}`, {
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                let errorMsg = 'Failed to fetch user details';
                try {
                    const errorData = await response.json();
                    errorMsg = errorData.error || errorMsg;
                } catch {
                    // response is not JSON (probably HTML)
                }
                if (response.status === 401) {
                    navigate('/login');
                    return;
                }
                throw new Error(errorMsg);
            }

            const data = await response.json();
            setAccount({
                accountBalance: data.accountBalance,
                profit: data.profit,
                username: data.username
            });
        } catch (err) {
            setError((err as Error).message);
        }
    };

    const fetchAvailableAssets = async () => {
        try {
            const response = await fetch("http://localhost:8000/api/v1/assets");
            if (!response.ok) throw new Error("Nie udało się pobrać dostępnych aktywów");
            const data = await response.json();
            setAvailableAssets(data);
        } catch (err) {
            // ignore
        }
    };

    useEffect(() => {
        fetchWalletDetails();
        fetchAvailableAssets();
        fetchAccountInfo();
        const interval = setInterval(() => {
            fetchAccountInfo();
        }, 10000);
        return () => clearInterval(interval);
    }, [userId]);

    useEffect(() => {
        if (walletLayoutRef.current) {
            const navbarHeight = 64;
            walletLayoutRef.current.scrollTo({ top: 0 });
            if (window.scrollY < navbarHeight) {
                window.scrollTo({ top: 0 });
            }
        }
    }, [loading, error]);

    const handleAddFunds = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const response = await fetch(`http://localhost:8000/api/v1/users/${userId}/add-funds`, {
                method: "POST",
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ amount: parseFloat(addFundsAmount) })
            });

            if (!response.ok) {
                if (response.status === 401) {
                    navigate('/login');
                    return;
                }
                throw new Error('Failed to add funds');
            }

            const data = await response.json();
            setAccount({
                ...account,
                accountBalance: data.accountBalance
            });
            setAddFundsAmount("");
        } catch (err) {
            setError((err as Error).message);
        }
    };

    const handleTrade = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const response = await fetch(`http://localhost:8000/api/v1/users/${userId}/wallet/trade`, {
                method: "POST",
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    assetId: parseInt(tradeAssetId),
                    amount: parseFloat(tradeAmount),
                    type: tradeType
                })
            });

            if (!response.ok) {
                if (response.status === 401) {
                    navigate('/login');
                    return;
                }
                const data = await response.json();
                throw new Error(data.error || "Nie udało się wykonać transakcji");
            }

            setTradeAssetId("");
            setTradeAmount("");
            fetchWalletDetails();
            fetchAccountInfo();
            setError("");
        } catch (err) {
            setError((err as Error).message);
        }
    };

    if (loading) return <p>Ładowanie portfela...</p>;
    if (error) return <p>{error}</p>;

    return (
        <div
            className="wallet-layout wallet-main-container"
            ref={walletLayoutRef}
        >
            <aside className="wallet-sidebar">
                <div className="wallet-user">
                    <div className="wallet-avatar">
                        {account.username ? account.username[0].toUpperCase() : "U"}
                    </div>
                    <div className="wallet-username">{account.username || "Użytkownik"}</div>
                </div>
                <div className="wallet-balance">
                    <span>Stan konta</span>
                    <div className="wallet-balance-value">{account.accountBalance.toFixed(2)} PLN</div>
                </div>
                <div className="wallet-profit">
                    <span>Profit</span>
                    <div className={`wallet-profit-value ${account.profit >= 0 ? "profit-pos" : "profit-neg"}`}>
                        {account.profit.toFixed(2)} PLN
                    </div>
                </div>
                <form onSubmit={handleAddFunds} className="wallet-addfunds-form">
                    <input
                        type="number"
                        placeholder="Kwota do dodania"
                        value={addFundsAmount}
                        onChange={e => setAddFundsAmount(e.target.value)}
                        min="0.01"
                        step="0.01"
                        required
                        className="wallet-addfunds-input"
                    />
                    <button type="submit" className="wallet-addfunds-btn">+</button>
                </form>
            </aside>
            <main className="wallet-main">
                <h2 className="wallet-assets-title">Twoje aktywa</h2>
                {assets.length > 0 ? (
                    <div className="wallet-assets-list">
                        {assets.map((asset) => (
                            <div key={asset.id} className="wallet-asset-item">
                                <div className="wallet-asset-main">
                                    <span className="wallet-asset-symbol">{asset.symbol}</span>
                                    <span className="wallet-asset-name">{asset.name}</span>
                                </div>
                                <div className="wallet-asset-details">
                                    <span className="wallet-asset-amount">{asset.amount} szt.</span>
                                    <span className="wallet-asset-price">@ {asset.price} USD</span>
                                </div>
                            </div>
                        ))}
                    </div>
                ) : (
                    <p className="wallet-assets-empty">Brak aktywów w portfelu.</p>
                )}

                <div className="wallet-trade-section">
                    <h3>Kup/Sprzedaj aktywo</h3>
                    <form onSubmit={handleTrade} className="wallet-trade-form">
                        <select
                            value={tradeAssetId}
                            onChange={e => setTradeAssetId(e.target.value)}
                            required
                            className="wallet-trade-select"
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
                            className="wallet-trade-input"
                        />
                        <select
                            value={tradeType}
                            onChange={e => setTradeType(e.target.value as "BUY" | "SELL")}
                            className="wallet-trade-type"
                        >
                            <option value="BUY">Kup</option>
                            <option value="SELL">Sprzedaj</option>
                        </select>
                        <button type="submit" className="wallet-trade-btn">Wykonaj</button>
                    </form>
                    {selectedTradeAsset && parsedTradeAmount > 0 && (
                        <div className="wallet-trade-preview">
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
                <div className="wallet-tip">
                    <span>💡 Tip: Regularnie sprawdzaj swój profit i reaguj na zmiany rynku!</span>
                </div>
            </main>
        </div>
    );
}

export default UserWallet;
