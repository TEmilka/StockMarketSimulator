import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { useStore } from "../store";
import { useNavigate } from "react-router-dom";
import './Users.css';
import './UsersAdminCustom.css';

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
    const [isAdmin, setIsAdmin] = useState(false);
    const navigate = useNavigate();

    useEffect(() => {
        const checkAdminStatus = () => {
            const token = localStorage.getItem("accessToken");
            if (token) {
                try {
                    const payload = JSON.parse(atob(token.split(".")[1]));
                    setIsAdmin(payload.authorities === "ROLE_ADMIN");
                } catch (err) {
                    setIsAdmin(false);
                }
            }
        };
        checkAdminStatus();
    }, []);

    const fetchUsers = async () => {
        try {
            const accessToken = localStorage.getItem("accessToken");
            if (!accessToken) throw new Error("Brak autoryzacji. Zaloguj się ponownie.");
            const response = await fetch("http://localhost:8000/api/users", {
                headers: { 'Authorization': `Bearer ${accessToken}` }
            });
            if (response.status === 401) throw new Error("Sesja wygasła. Zaloguj się ponownie.");
            if (response.status === 403) throw new Error("Brak uprawnień do wyświetlania użytkowników.");
            if (!response.ok) throw new Error("Nie udało się pobrać użytkowników");
            const data = await response.json();
            setUsers(data);
        } catch (err) {
            setError((err as Error).message);
        }
    };

    useEffect(() => {
        fetchUsers();
    }, [isAdmin]);

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
                    wallet: { assets: {} }
                }),
            });
            if (!response.ok) throw new Error("Nie udało się dodać użytkownika");
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
            const accessToken = localStorage.getItem("accessToken");
            if (!accessToken) throw new Error("Brak autoryzacji. Zaloguj się ponownie.");
            const response = await fetch(`http://localhost:8000/api/users/${id}`, {
                method: "DELETE",
                headers: { 'Authorization': `Bearer ${accessToken}` }
            });
            if (response.status === 401) throw new Error("Sesja wygasła. Zaloguj się ponownie.");
            if (response.status === 403) throw new Error("Brak uprawnień do usuwania użytkowników.");
            if (!response.ok) throw new Error("Nie udało się usunąć użytkownika");
            setUsers(users.filter(user => user.id !== id));
        } catch (error) {
            setError((error as Error).message);
        }
    };

    const viewUserWallet = (userId: number) => {
        navigate(`/user-wallet/${userId}`);
    };

    return (
        <div className="admin-users-container">
            <h1 className="admin-users-title">Panel administratora – Zarządzanie użytkownikami</h1>
            <p className="admin-users-desc">
                Tutaj możesz przeglądać, dodawać i usuwać użytkowników oraz podejrzeć ich portfele inwestycyjne.
            </p>
            {!isAdmin ? (
                <div className="error-message">
                    Nie masz uprawnień do wyświetlania listy użytkowników.
                    Tylko administrator ma dostęp do tej funkcjonalności.
                </div>
            ) : (
                <>
                    <form onSubmit={handleSubmit(addUserToApi)} className="admin-users-form">
                        <input
                            {...register("name", { required: true })}
                            placeholder="Imię"
                            className="admin-users-input"
                        />
                        <input
                            {...register("email", { required: true })}
                            placeholder="Email"
                            type="email"
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

                    {error && <p className="admin-users-error">{error}</p>}

                    <div className="admin-users-list-section">
                        <h2 className="admin-users-list-title">Lista użytkowników</h2>
                        {users.length > 0 ? (
                            <div className="admin-users-list">
                                {users.map((user: User, idx: number) => (
                                    <div key={user.id || idx} className="admin-user-item">
                                        <div className="admin-user-avatar">
                                            {user.name ? user.name[0].toUpperCase() : "U"}
                                        </div>
                                        <div className="admin-user-info">
                                            <span className="admin-user-name">{user.name}</span>
                                            <span className="admin-user-email">{user.email}</span>
                                        </div>
                                        <div className="admin-user-actions">
                                            <button className="admin-user-wallet-btn" onClick={() => viewUserWallet(user.id!)}>Portfel</button>
                                            <button className="admin-user-delete-btn" onClick={() => deleteUser(user.id!)}>Usuń</button>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        ) : (
                            <p className="admin-users-empty">Brak użytkowników</p>
                        )}
                    </div>
                </>
            )}
        </div>
    );
}

export default Users;
