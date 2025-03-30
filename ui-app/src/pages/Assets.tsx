import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import './Assets.css';

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

function Assets() {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [assets, setAssets] = useState<Asset[]>([]);
    const { register, handleSubmit, reset } = useForm<AssetForm>();

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
    }, []);

    const addAssetToApi = async (data: AssetForm) => {
        setLoading(true);
        setError("");
        try {
            const response = await fetch("http://localhost:8000/api/assets", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    symbol: data.symbol,
                    name: data.name,
                    price: data.price,
                }),
            });

            if (!response.ok) {
                throw new Error("Nie udało się dodać assetu");
            }

            reset();
            fetchAssets();
        } catch (err) {
            setError((err as Error).message);
        } finally {
            setLoading(false);
        }
    };

    const deleteAsset = async (id: number) => {
        try {
            const response = await fetch(`http://localhost:8000/api/assets/${id}`, {
                method: "DELETE",
            });

            if (response.ok) {
                setAssets(assets.filter(asset => asset.id !== id));
            } else {
                setError("Nie udało się usunąć assetu");
            }
        } catch (err) {
            setError((err as Error).message);
        }
    };

    return (
        <div className="container">
            <h1 className="title">Dodaj Asset</h1>

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

            {error && <p className="error">{error}</p>}

            <div className="asset-list">
                <h2 className="asset-list-title">Lista assetów</h2>
                {assets.length > 0 ? (
                    <ul>
                        {assets.map((asset: Asset, idx: number) => (
                            <li key={idx} className="asset-item">
                                {asset.name} ({asset.symbol}) - {asset.price} USD
                                <button onClick={() => deleteAsset(asset.id)}>Usuń</button>
                            </li>
                        ))}
                    </ul>
                ) : (
                    <p>Brak assetów</p>
                )}
            </div>
        </div>
    );
}

export default Assets;
