import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { useStore } from "../store";
import { useNavigate } from "react-router-dom";
import './Users.css';

interface User {
    id?: number;
    name: string;
    email: string;
}

interface UserForm {
    name: string;
    email: string;
}

function Users() {
    const { users, setUsers, addUser } = useStore();
    const { register, handleSubmit, reset } = useForm<UserForm>();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const navigate = useNavigate();

    const fetchUsers = async () => {
        try {
            const response = await fetch("http://localhost:8000/api/users");
            if (!response.ok) {
                throw new Error("Nie udało się pobrać użytkowników");
            }
            const data = await response.json();
            setUsers(data);
        } catch (err) {
            setError((err as Error).message);
        }
    };

    useEffect(() => {
        fetchUsers();
    }, []);

    const addUserToApi = async (data: UserForm) => {
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
            addUser(newUser);
            reset();
        } catch (err) {
            setError((err as Error).message);
        } finally {
            setLoading(false);
        }
    };

    const deleteUser = async (id: number) => {
        try {
            const response = await fetch(`http://localhost:8000/api/users/${id}`, {
                method: "DELETE",
            });

            if (response.ok) {
                setUsers(users.filter(user => user.id !== id));
            } else {
                setError("Nie udało się usunąć użytkownika");
            }
        } catch (err) {
            setError((err as Error).message);
        }
    };

    const viewUserWallet = (userId: number) => {
        navigate(`/user-wallet/${userId}`);
    };

    return (
        <div className="container">
            <h1 className="title">Stock Market Simulator</h1>

            <form onSubmit={handleSubmit(addUserToApi)} className="form">
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
                                <button onClick={() => deleteUser(user.id!)}>Usuń</button>
                                <button onClick={() => viewUserWallet(user.id!)}>Zobacz portfel</button>
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

export default Users;
