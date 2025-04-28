import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

interface User {
  id: number;
  name: string;
  email: string;
}

function UserList() {
  const [users, setUsers] = useState<User[]>([]);
  const [error, setError] = useState<string>('');
  const navigate = useNavigate();

  const fetchUsers = async () => {
    try {
      const response = await fetch('http://localhost:8000/api/users', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
        }
      });
      
      if (response.status === 403) {
        setError('Nie masz uprawnień do wyświetlenia tej zawartości');
        return;
      }
      
      if (!response.ok) {
        throw new Error('Wystąpił błąd podczas pobierania danych');
      }
      
      const data = await response.json();
      setUsers(data);
    } catch (error) {
      setError('Wystąpił błąd podczas pobierania użytkowników');
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleDelete = async (id: number) => {
    try {
      const response = await fetch(`http://localhost:8000/api/users/${id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
        }
      });

      if (!response.ok) throw new Error('Nie udało się usunąć użytkownika');
      setUsers(users.filter(user => user.id !== id));
    } catch (error) {
      setError('Wystąpił błąd podczas usuwania użytkownika');
    }
  };

  const handleViewWallet = (id: number) => {
    navigate(`/user-wallet/${id}`);
  };

  if (error) {
    return <div className="error-message">{error}</div>;
  }

  return (
    <div className="user-list">
      <h2>Lista użytkowników</h2>
      {users.map((user) => (
        <div key={user.id} className="user-item">
          <span>{user.name} - {user.email}</span>
          <button onClick={() => handleDelete(user.id)}>Usuń</button>
          <button onClick={() => handleViewWallet(user.id)}>Zobacz portfel</button>
        </div>
      ))}
    </div>
  );
}

export default UserList;
