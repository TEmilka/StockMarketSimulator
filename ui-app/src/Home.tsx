import { Link } from "react-router-dom";
import './App.css';

function Home() {
    return (
        <div className="container">
            <h1 className="title">Stock Market Simulator</h1>

            <div className="button-container">
                <Link to="/add-user">
                    <button className="button">
                        Użytkownicy
                    </button>
                </Link>
                <Link to="/add-asset">
                    <button className="button">
                        Aktywa
                    </button>
                </Link>
            </div>
        </div>
    );
}

export default Home;
