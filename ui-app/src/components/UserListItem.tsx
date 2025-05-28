import React from "react";

interface User {
    id?: number;
    name: string;
    email: string;
}

interface Props {
    user: User;
    onWalletClick: (id: number) => void;
    onTransactionsClick: (id: number) => void;
    onDeleteClick: (id: number) => void;
}

const UserListItem: React.FC<Props> = ({ user, onWalletClick, onTransactionsClick, onDeleteClick }) => (
    <div className="admin-user-item">
        <div className="admin-user-avatar">
            {user.name ? user.name[0].toUpperCase() : "U"}
        </div>
        <div className="admin-user-info">
            <span className="admin-user-name">{user.name}</span>
            <span className="admin-user-email">{user.email}</span>
        </div>
        <div className="admin-user-actions">
            <button className="admin-user-wallet-btn" onClick={() => onWalletClick(user.id!)}>Portfel</button>
            <button className="admin-user-transactions-btn" onClick={() => onTransactionsClick(user.id!)}>Transakcje</button>
            <button className="admin-user-delete-btn" onClick={() => onDeleteClick(user.id!)}>Usuń</button>
        </div>
    </div>
);

export default UserListItem;
