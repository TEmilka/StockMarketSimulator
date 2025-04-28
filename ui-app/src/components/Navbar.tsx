import { Link, useNavigate } from "react-router-dom";
import './Navbar.css';

function Navbar() {
    const navigate = useNavigate();
    const accessToken = localStorage.getItem("accessToken");

    const handleMyAccountClick = () => {
        if (accessToken) {
            navigate(`/user-wallet/${getUserIdFromToken(accessToken)}`);
        } else {
            navigate("/login");
        }
    };

    const handleLogout = () => {
        localStorage.removeItem("accessToken");
        navigate("/login");
    };

    const getUserIdFromToken = (token: string): string => {
        // Zakładamy, że token jest w formacie JWT
        const payload = JSON.parse(atob(token.split(".")[1]));
        return payload.userId;
    };

    return (
        <nav className="navbar">
            <div className="navbar-container">
                <Link to="/" className="navbar-link navbar-left">Home</Link>
                <div className="navbar-right">
                    {accessToken && (
                        <button onClick={handleLogout} className="navbar-button">Logout</button>
                    )}
                    <button onClick={handleMyAccountClick} className="navbar-button">My Account</button>
                </div>
            </div>
        </nav>
    );
}

export default Navbar;