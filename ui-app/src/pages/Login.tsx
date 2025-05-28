import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useStore } from "../store";
import './Login.css';

function Login() {
    const [isRegistering, setIsRegistering] = useState(false);
    const [username, setUsername] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const navigate = useNavigate();
    const { setAuthenticated, setUserRole } = useStore();

    const handleLogin = async (e: React.FormEvent) => {
        e.preventDefault();
        setError("");
        try {
            const response = await fetch("http://localhost:8000/api/v1/auth/login", {
                method: "POST",
                credentials: 'include',
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username, password }),
            });

            if (!response.ok) {
                throw new Error("Bledne dane logowania lub użytkownik nie istnieje");
            }

            const data = await response.json();
            console.log('Login response:', data);
            localStorage.setItem("userId", data.userId);
            setAuthenticated(true);
            setUserRole(data.role);
            navigate("/");
        } catch (err) {
            setError((err as Error).message);
        }
    };

    const handleRegister = async (e: React.FormEvent) => {
        e.preventDefault();
        setError("");
        try {
            const response = await fetch("http://localhost:8000/api/v1/auth/register", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username, email, password }),
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText);
            }

            setIsRegistering(false);
            setUsername("");
            setEmail("");
            setPassword("");
        } catch (err) {
            setError((err as Error).message);
        }
    };

    return (
        <div className="login-main-bg">
            <div className="login-container">
                <h1 className="login-title">{isRegistering ? "Rejestracja" : "Logowanie"}</h1>
                <div className="login-subtitle">
                    {isRegistering
                        ? "Załóż nowe konto, aby korzystać z symulatora giełdy."
                        : "Zaloguj się, aby zarządzać swoim portfelem inwestycyjnym."}
                </div>
                <form onSubmit={isRegistering ? handleRegister : handleLogin} className="login-form">
                    <input
                        type="text"
                        placeholder="Nazwa użytkownika"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        required
                    />
                    {isRegistering && (
                        <input
                            type="email"
                            placeholder="Email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                        />
                    )}
                    <input
                        type="password"
                        placeholder="Hasło"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                    />
                    <button type="submit" className="login-button">
                        {isRegistering ? "Zarejestruj się" : "Zaloguj się"}
                    </button>
                </form>
                {error && <p className="error">{error}</p>}
                <p className="toggle-text">
                    {isRegistering ? "Masz już konto?" : "Nie masz konta?"}{" "}
                    <button
                        type="button"
                        className="toggle-button"
                        onClick={() => setIsRegistering(!isRegistering)}
                    >
                        {isRegistering ? "Zaloguj się" : "Zarejestruj się"}
                    </button>
                </p>
            </div>
        </div>
    );
}

export default Login;
