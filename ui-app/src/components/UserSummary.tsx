interface UserAccount {
    accountBalance: number;
    profit: number;
    username?: string;
}

interface Props {
    account: UserAccount;
    addFundsAmount: string;
    setAddFundsAmount: (v: string) => void;
    handleAddFunds: (e: React.FormEvent) => void;
}

const UserSummary: React.FC<Props> = ({
    account,
    addFundsAmount,
    setAddFundsAmount,
    handleAddFunds
}) => (
    <div className="wallet-user">
        <div className="wallet-avatar">
            {account.username ? account.username[0].toUpperCase() : "U"}
        </div>
        <div className="wallet-username">{account.username || "Użytkownik"}</div>
        <div className="wallet-balance">
            <span>Stan konta</span>
            <div className="wallet-balance-value">{account.accountBalance.toFixed(2)} PLN</div>
        </div>
        <div className="wallet-profit">
            <span>Profit</span>
            <div className={`wallet-profit-value ${account.profit >= 0 ? "profit-pos" : "profit-neg"}`}>
                {account.profit.toFixed(2)} PLN
            </div>
        </div>
        <form onSubmit={handleAddFunds} className="wallet-addfunds-form">
            <input
                type="number"
                placeholder="Kwota do dodania"
                value={addFundsAmount}
                onChange={e => setAddFundsAmount(e.target.value)}
                min="0.01"
                step="0.01"
                required
                className="wallet-addfunds-input"
            />
            <button type="submit" className="wallet-addfunds-btn">+</button>
        </form>
    </div>
);

export default UserSummary;
