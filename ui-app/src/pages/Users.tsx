import { useState, useEffect } from "react";
import { useStore } from "../store";
import { useNavigate } from "react-router-dom";
import './Users.css';
import './UsersAdminCustom.css';
import UserAddForm from "../components/UserAddForm";
import UserList from "../components/UserList";
import UserTransactionsList from "../components/UserTransactionsList";

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

interface UserForm {
    username: string;
    email: string;
    password: string;
}

function Users() {
    const { users, setUsers, userRole, addUser } = useStore();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [selectedUser, setSelectedUser] = useState<number | null>(null);
    const [transactions, setTransactions] = useState<Transaction[]>([]);
    const [loadingTransactions, setLoadingTransactions] = useState(false);
    const navigate = useNavigate();

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
            // Backend oczekuje "username"
            const payload = {
                username: data.username,
                email: data.email,
                password: data.password
            };
            const response = await fetch("http://localhost:8000/api/v1/users", {
                method: "POST",
                credentials: 'include',
                headers: { 
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(payload),
            });

            if (!response.ok) {
                throw new Error("Nie udało się dodać użytkownika");
            }

            const newUser = await response.json();
            addUser(newUser);
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

    const viewUserWallet = async (userId: number) => {
        try {
            // Prefetch wallet data before navigation
            const response = await fetch(`http://localhost:8000/api/v1/users/${userId}/wallet/details`, {
                credentials: 'include'
            });
            if (response.ok) {
                // Store in global state or localStorage for instant access after navigation
                localStorage.setItem(`wallet_${userId}`, JSON.stringify(await response.json()));
            }
            navigate(`/user-wallet/${userId}`);
        } catch (err) {
            console.error("Failed to prefetch wallet data:", err);
            navigate(`/user-wallet/${userId}`);
        }
    };

    const fetchTransactions = async (userId: number) => {
        setLoadingTransactions(true);
        try {
            const [userResponse, transactionsResponse] = await Promise.all([
                fetch(`http://localhost:8000/api/v1/users/${userId}`, {
                    credentials: 'include'
                }),
                fetch(`http://localhost:8000/api/v1/users/${userId}/transactions`, {
                    credentials: 'include'
                })
            ]);

            if (!userResponse.ok || !transactionsResponse.ok) {
                throw new Error("Nie udało się pobrać danych użytkownika");
            }

            const [userData, transactionsData] = await Promise.all([
                userResponse.json(),
                transactionsResponse.json()
            ]);

            setTransactions(transactionsData);
            setSelectedUser(userId);
            
            // Cache user data
            localStorage.setItem(`user_${userId}`, JSON.stringify(userData));
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

            <UserAddForm loading={loading} onAdd={addUserToApi} />

            {error && <p className="admin-users-error">{error}</p>}

            <UserList
                users={users}
                loading={loading}
                onWalletClick={viewUserWallet}
                onTransactionsClick={fetchTransactions}
                onDeleteClick={deleteUser}
            />

            {selectedUser && (
                <UserTransactionsList
                    transactions={transactions}
                    loading={loadingTransactions}
                />
            )}
        </div>
    );
}

export default Users;
