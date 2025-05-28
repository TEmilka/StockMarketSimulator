import { useState, useEffect, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import "./UserWalletCustom.css";
import UserSummary from "../components/UserSummary";
import WalletAssetsList from "../components/WalletAssetsList";
import TradeForm from "../components/TradeForm";
import WalletTip from "../components/WalletTip";

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
                <UserSummary
                    account={account}
                    addFundsAmount={addFundsAmount}
                    setAddFundsAmount={setAddFundsAmount}
                    handleAddFunds={handleAddFunds}
                />
            </aside>
            <main className="wallet-main">
                <h2 className="wallet-assets-title">Twoje aktywa</h2>
                <WalletAssetsList assets={assets} />
                <TradeForm
                    availableAssets={availableAssets}
                    tradeAssetId={tradeAssetId}
                    setTradeAssetId={setTradeAssetId}
                    tradeAmount={tradeAmount}
                    setTradeAmount={setTradeAmount}
                    tradeType={tradeType}
                    setTradeType={setTradeType}
                    handleTrade={handleTrade}
                    selectedTradeAsset={selectedTradeAsset}
                    parsedTradeAmount={parsedTradeAmount}
                    tradeValue={tradeValue}
                />
                <WalletTip />
            </main>
        </div>
    );
}

export default UserWallet;
