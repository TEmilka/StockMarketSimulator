import UserTransactionItem from "./UserTransactionItem";

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

interface Props {
    transactions: Transaction[];
    loading: boolean;
}

const UserTransactionsList: React.FC<Props> = ({ transactions, loading }) => (
    <div className="admin-users-transactions-section">
        <h2 className="admin-users-list-title">Historia transakcji użytkownika</h2>
        {loading ? (
            <p className="admin-users-loading">Ładowanie transakcji...</p>
        ) : transactions.length > 0 ? (
            <div className="admin-users-transactions">
                {transactions.map((transaction) => (
                    <UserTransactionItem key={transaction.id} transaction={transaction} />
                ))}
            </div>
        ) : (
            <p className="admin-users-empty">Brak transakcji</p>
        )}
    </div>
);

export default UserTransactionsList;
