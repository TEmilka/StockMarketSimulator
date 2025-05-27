import { Link, useNavigate } from "react-router-dom";
import { useStore } from "../store";
import './Navbar.css';

function Navbar() {
    const navigate = useNavigate();
    const { isAuthenticated, logout } = useStore();
    const userId = localStorage.getItem("userId");

    const handleMyAccountClick = () => {
        if (isAuthenticated && userId) {
            navigate(`/user-wallet/${userId}`);
        } else {
            navigate("/login");
        }
    };

    const handleLogout = async () => {
        try {
            const response = await fetch("http://localhost:8000/api/auth/logout", {
                method: "POST",
                credentials: 'include',
            });
            
            if (response.ok) {
                logout();
                navigate("/login");
            }
        } catch (error) {
            console.error("Logout failed:", error);
        }
    };

    return (
        <nav className="navbar">
            <div className="navbar-container">
                <Link to="/" className="navbar-link navbar-left">Home</Link>
                <div className="navbar-right">
                    {isAuthenticated && (
                        <button onClick={handleLogout} className="navbar-button">Logout</button>
                    )}
                    <button onClick={handleMyAccountClick} className="navbar-button">My Account</button>
                    <button onClick={() => navigate('/notifications')} className="navbar-button">Powiadomienia</button>
                </div>
            </div>
        </nav>
    );
}

export default Navbar;