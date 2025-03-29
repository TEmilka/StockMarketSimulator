import { useState } from "react";
import { useForm } from "react-hook-form";
import { useStore } from "../store";
import './AddUser.css';

interface User {
    id?: number;
    name: string;
    email: string;
}

interface UserForm {
    name: string;
    email: string;
}

function AddUser() {
    const { users, setUsers } = useStore();
    const { register, handleSubmit, reset } = useForm<UserForm>();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const addUser = async (data: UserForm) => {
        setLoading(true);
        setError("");
        try {
            const response = await fetch("http://localhost:8000/api/users", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    name: data.name,
                    email: data.email,
                    wallet: {
                        assets: {}
                    }
                }),
            });

            if (!response.ok) {
                throw new Error("Nie udało się dodać użytkownika");
            }

            const newUser: User = await response.json();
            setUsers([...users, newUser]);
            reset();
        } catch (err) {
            setError((err as Error).message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="container">
            <h1 className="title">Stock Market Simulator</h1>

            <form onSubmit={handleSubmit(addUser)} className="form">
                <input
                    {...register("name", { required: true })}
                    placeholder="Imię"
                    className="input"
                />
                <input
                    {...register("email", { required: true })}
                    placeholder="Email"
                    type="email"
                    className="input input-email"
                />
                <button
                    type="submit"
                    className="button"
                    disabled={loading}
                >
                    {loading ? "Dodawanie..." : "Dodaj użytkownika"}
                </button>
            </form>

            {error && <p className="error">{error}</p>}

            <div className="user-list">
                <h2 className="user-list-title">Lista użytkowników</h2>
                {users.length > 0 ? (
                    <ul>
                        {users.map((user: User, idx: number) => (
                            <li key={idx} className="user-item">
                                {user.name} - {user.email}
                            </li>
                        ))}
                    </ul>
                ) : (
                    <p>Brak użytkowników</p>
                )}
            </div>
        </div>
    );
}

export default AddUser;
