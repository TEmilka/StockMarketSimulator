import { UseFormRegister } from "react-hook-form";

interface AssetForm {
    symbol: string;
    name: string;
    price: number;
}

interface Props {
    loading: boolean;
    onAdd: (e?: React.BaseSyntheticEvent) => void;
    register: UseFormRegister<AssetForm>;
}

const AssetAddForm: React.FC<Props> = ({ loading, onAdd, register }) => (
    <form onSubmit={onAdd} className="assets-form">
        <input
            {...register("symbol", { required: true })}
            placeholder="Symbol"
            className="assets-input"
        />
        <input
            {...register("name", { required: true })}
            placeholder="Nazwa"
            className="assets-input"
        />
        <input
            {...register("price", { required: true })}
            placeholder="Cena"
            type="number"
            className="assets-input"
        />
        <button
            type="submit"
            className="assets-btn"
            disabled={loading}
        >
            {loading ? "Dodawanie..." : "Dodaj asset"}
        </button>
    </form>
);

export default AssetAddForm;
