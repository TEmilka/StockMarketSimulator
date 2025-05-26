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
    username: string;  // changed from name to username
    email: string;
    password: string;
}

interface Transaction {
    id: number;
    type: 'BUY' | 'SELL';
    assetSymbol: string;
    assetName: string;
    amount: number;
    price: number;
    totalValue: number;
    timestamp: string;
}

function Users() {
    const { users, setUsers, addUser } = useStore();
    const { register, handleSubmit, reset } = useForm<UserForm>();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [isAdmin, setIsAdmin] = useState(false);
    const [selectedUser, setSelectedUser] = useState<number | null>(null);
    const [transactions, setTransactions] = useState<Transaction[]>([]);
    const [loadingTransactions, setLoadingTransactions] = useState(false);
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
            const accessToken = localStorage.getItem("accessToken");
            if (!accessToken) throw new Error("Brak autoryzacji. Zaloguj się ponownie.");

            const response = await fetch("http://localhost:8000/api/users", {
                method: "POST",
                headers: { 
                    "Content-Type": "application/json",
                    'Authorization': `Bearer ${accessToken}`
                },
                body: JSON.stringify({
                    username: data.username,  // added comma here
                    email: data.email,
                    password: data.password,
                    wallet: { assets: {} }
                }),
            });

            if (response.status === 401) throw new Error("Sesja wygasła. Zaloguj się ponownie.");
            if (response.status === 403) throw new Error("Brak uprawnień do dodawania użytkowników.");
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

    const fetchTransactions = async (userId: number) => {
        setLoadingTransactions(true);
        try {
            const accessToken = localStorage.getItem("accessToken");
            if (!accessToken) throw new Error("Brak autoryzacji. Zaloguj się ponownie.");
            
            const response = await fetch(`http://localhost:8000/api/users/${userId}/transactions`, {
                headers: { 'Authorization': `Bearer ${accessToken}` }
            });
            
            if (!response.ok) throw new Error("Nie udało się pobrać transakcji");
            const data = await response.json();
            setTransactions(data);
            setSelectedUser(userId);
        } catch (err) {
            setError((err as Error).message);
        } finally {
            setLoadingTransactions(false);
        }
    };

    return (
        <div className="admin-users-container">
            <div className="admin-users-header">
                <h1 className="admin-users-title">Panel administratora – Zarządzanie użytkownikami</h1>
                <p className="admin-users-desc">
                    Tutaj możesz przeglądać, dodawać i usuwać użytkowników oraz podejrzeć ich portfele inwestycyjne.
                </p>
            </div>
            {!isAdmin ? (
                <div className="error-message">
                    Nie masz uprawnień do wyświetlania listy użytkowników.
                    Tylko administrator ma dostęp do tej funkcjonalności.
                </div>
            ) : (
                <>
                    <form onSubmit={handleSubmit(addUserToApi)} className="admin-users-form">
                        <input
                            {...register("username", { required: true })}  // changed from name to username
                            placeholder="Nazwa użytkownika"  // changed placeholder text
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
                                            <button className="admin-user-transactions-btn" onClick={() => fetchTransactions(user.id!)}>Transakcje</button>
                                            <button className="admin-user-delete-btn" onClick={() => deleteUser(user.id!)}>Usuń</button>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        ) : (
                            <p className="admin-users-empty">Brak użytkowników</p>
                        )}
                    </div>

                    {selectedUser && (
                        <div className="admin-users-transactions-section">
                            <h2 className="admin-users-list-title">Historia transakcji użytkownika</h2>
                            {loadingTransactions ? (
                                <p className="admin-users-loading">Ładowanie transakcji...</p>
                            ) : transactions.length > 0 ? (
                                <div className="admin-users-transactions">
                                    {transactions.map((transaction) => (
                                        <div key={transaction.id} className="admin-user-transaction">
                                            <div className="transaction-type" data-type={transaction.type}>
                                                {transaction.type === 'BUY' ? 'Kupno' : 'Sprzedaż'}
                                            </div>
                                            <div className="transaction-details">
                                                <span className="transaction-asset">
                                                    {transaction.assetName} ({transaction.assetSymbol})
                                                </span>
                                                <span className="transaction-amount">
                                                    {transaction.amount} szt. @ {transaction.price.toFixed(2)} USD
                                                </span>
                                                <span className="transaction-total">
                                                    Wartość: {transaction.totalValue.toFixed(2)} USD
                                                </span>
                                            </div>
                                            <div className="transaction-timestamp">
                                                {new Date(transaction.timestamp).toLocaleString()}
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            ) : (
                                <p className="admin-users-empty">Brak transakcji</p>
                            )}
                        </div>
                    )}
                </>
            )}
        </div>
    );
}

export default Users;
