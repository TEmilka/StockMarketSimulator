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
    transaction: Transaction;
}

const UserTransactionItem: React.FC<Props> = ({ transaction }) => (
    <div className="admin-user-transaction">
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
);

export default UserTransactionItem;
