import { Link } from "react-router-dom";
import './App.css';
import './Home.css';

function Home() {
    return (
        <div className="home-main-container">
            <div className="home-header">
                <h1 className="home-title">Stock Market Simulator</h1>
                <p className="home-subtitle">
                    Symulator giełdy, w którym możesz kupować i sprzedawać aktywa, śledzić ich ceny w czasie rzeczywistym oraz zarządzać swoim portfelem inwestycyjnym.<br />
                    <span className="home-highlight">Zaloguj się, aby rozpocząć inwestowanie!</span>
                </p>
            </div>
            <div className="home-assets-section">
                <Link to="/add-asset">
                    <button className="home-assets-btn">
                        Przeglądaj Aktywa
                    </button>
                </Link>
                <div className="home-assets-info">
                    <h3>Co możesz zrobić?</h3>
                    <ul>
                        <li>Obserwuj aktualne ceny akcji, kryptowalut i innych aktywów</li>
                        <li>Analizuj wykresy historyczne cen</li>
                        <li>Kupuj i sprzedawaj aktywa do swojego portfela</li>
                        <li>Śledź swój zysk i saldo konta na bieżąco</li>
                    </ul>
                </div>
            </div>
            <div className="home-footer">
                <Link to="/add-user">
                    <button className="home-admin-btn">
                        Panel użytkowników (admin)
                    </button>
                </Link>
                <div className="home-footer-info">
                    <span>Panel użytkowników dostępny tylko dla administratorów.</span>
                </div>
            </div>
        </div>
    );
}

export default Home;
