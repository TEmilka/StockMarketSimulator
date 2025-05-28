import UserListItem from './UserListItem';

interface User {
  id?: number;
  name: string;
  email: string;
}

export interface UserListProps {
  users: User[];
  loading: boolean;
  onWalletClick: (id: number) => void | Promise<void>;
  onTransactionsClick: (id: number) => void | Promise<void>;
  onDeleteClick: (id: number) => void | Promise<void>;
}

const UserList: React.FC<UserListProps> = ({
  users,
  loading,
  onWalletClick,
  onTransactionsClick,
  onDeleteClick
}) => (
  <div className="admin-users-list-section">
    <h2 className="admin-users-list-title">Lista użytkowników</h2>
    {loading ? (
      <p className="admin-users-loading">Ładowanie użytkowników...</p>
    ) : users.length > 0 ? (
      <div className="admin-users-list">
        {users.map((user) => (
          <UserListItem
            key={user.id}
            user={user}
            onWalletClick={onWalletClick}
            onTransactionsClick={onTransactionsClick}
            onDeleteClick={onDeleteClick}
          />
        ))}
      </div>
    ) : (
      <p className="admin-users-empty">Brak użytkowników</p>
    )}
  </div>
);

export default UserList;
