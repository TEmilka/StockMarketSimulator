import { Link, useNavigate } from "react-router-dom";
import './Navbar.css';

function Navbar() {
    const navigate = useNavigate();
    const accessToken = localStorage.getItem("accessToken");

    const handleMyAccountClick = () => {
        const userId = localStorage.getItem("userId");
        if (accessToken && userId) {
            navigate(`/user-wallet/${userId}`);
        } else {
            navigate("/login");
        }
    };

    const handleLogout = () => {
        localStorage.removeItem("accessToken");
        localStorage.removeItem("userId");
        navigate("/login");
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