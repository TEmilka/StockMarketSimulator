import { useForm } from "react-hook-form";

interface UserForm {
    username: string;
    email: string;
    password: string;
}

interface Props {
    loading: boolean;
    onAdd: (data: UserForm) => void | Promise<void>;
}

const UserAddForm: React.FC<Props> = ({ loading, onAdd }) => {
    const { register, handleSubmit, reset } = useForm<UserForm>();

    const handleAdd = async (data: UserForm) => {
        await onAdd(data);
        reset();
    };

    return (
        <form onSubmit={handleSubmit(handleAdd)} className="admin-users-form">
            <input
                {...register("username", { required: true })}
                placeholder="Nazwa użytkownika"
                className="admin-users-input"
            />
            <input
                {...register("email", { required: true })}
                placeholder="Email"
                type="email"
                className="admin-users-input"
            />
            <input
                {...register("password", { required: true })}
                placeholder="Hasło"
                type="password"
                className="admin-users-input"
            />
            <button
                type="submit"
                className="admin-users-btn"
                disabled={loading}
            >
                {loading ? "Dodawanie..." : "Dodaj użytkownika"}
            </button>
        </form>
    );
};

export default UserAddForm;
