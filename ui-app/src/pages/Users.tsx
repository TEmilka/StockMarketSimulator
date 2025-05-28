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
    const { users, setUsers, userRole, addUser } = useStore();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [selectedUser, setSelectedUser] = useState<number | null>(null);
    const [transactions, setTransactions] = useState<Transaction[]>([]);
    const [loadingTransactions, setLoadingTransactions] = useState(false);
    const navigate = useNavigate();
    const { register, handleSubmit, reset } = useForm<UserForm>();

    const fetchUsers = async () => {
        try {
            const response = await fetch("http://localhost:8000/api/v1/users", {
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                const errorData = await response.json();
                if (response.status === 401) {
                    navigate('/login');
                    return;
                }
                if (response.status === 403) {
                    setError("Nie masz uprawnień do wyświetlania listy użytkowników. Tylko administrator ma dostęp do tej funkcjonalności.");
                    return;
                }
                throw new Error(errorData.error || "Nie udało się pobrać użytkowników");
            }

            const data = await response.json();
            setUsers(data);
        } catch (err) {
            setError((err as Error).message);
        }
    };

    useEffect(() => {
        if (userRole === 'ROLE_ADMIN') {
            fetchUsers();
        }
    }, [userRole]);

    const addUserToApi = async (data: UserForm) => {
        setLoading(true);
        setError("");
        try {
            const response = await fetch("http://localhost:8000/api/v1/users", {
                method: "POST",
                credentials: 'include',
                headers: { 
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(data),
            });

            if (!response.ok) {
                throw new Error("Nie udało się dodać użytkownika");
            }

            const newUser = await response.json();
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
            const response = await fetch(`http://localhost:8000/api/v1/users/${id}`, {
                method: "DELETE",
                credentials: 'include',
                headers: { "Content-Type": "application/json" }
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
            const response = await fetch(`http://localhost:8000/api/v1/users/${userId}/transactions`, {
                credentials: 'include',
                headers: { "Content-Type": "application/json" }
            });

            if (response.status === 401) throw new Error("Sesja wygasła. Zaloguj się ponownie.");
            if (response.status === 403) throw new Error("Brak uprawnień do wyświetlania transakcji.");
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

    if (userRole !== 'ROLE_ADMIN') {
        return (
            <div className="admin-users-container">
                <div className="admin-users-header">
                    <h1 className="admin-users-title">Panel administratora</h1>
                    <div className="admin-users-error">
                        Nie masz uprawnień do wyświetlania listy użytkowników. 
                        Tylko administrator ma dostęp do tej funkcjonalności.
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="admin-users-container">
            <div className="admin-users-header">
                <h1 className="admin-users-title">Panel administratora – Zarządzanie użytkownikami</h1>
                <p className="admin-users-desc">
                    Tutaj możesz przeglądać, dodawać i usuwać użytkowników oraz podejrzeć ich portfele inwestycyjne.
                </p>
            </div>

            <form onSubmit={handleSubmit(addUserToApi)} className="admin-users-form">
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

            {error && <p className="admin-users-error">{error}</p>}

            <div className="admin-users-list-section">
                <h2 className="admin-users-list-title">Lista użytkowników</h2>
                {loading ? (
                    <p className="admin-users-loading">Ładowanie użytkowników...</p>
                ) : users.length > 0 ? (
                    <div className="admin-users-list">
                        {users.map((user: User) => (
                            <div key={user.id} className="admin-user-item">
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
        </div>
    );
}

export default Users;
