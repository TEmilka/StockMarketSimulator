// pages/AddAsset.tsx
import { useState } from "react";
import { useForm } from "react-hook-form";
import './AddAsset.css';

interface AssetForm {
    symbol: string;
    name: string;
    price: number;
}

function AddAsset() {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const { register, handleSubmit, reset } = useForm<AssetForm>();

    const addAsset = async (data: AssetForm) => {
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
        } catch (err) {
            setError((err as Error).message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="container">
            <h1 className="title">Dodaj Asset</h1>

            <form onSubmit={handleSubmit(addAsset)} className="form">
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
        </div>
    );
}

export default AddAsset;
