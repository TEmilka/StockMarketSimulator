import axios from 'axios';

const API_URL = 'http://localhost:8000/api/v1';

export const refreshToken = async () => {
    try {
        const response = await axios.post(
            `${API_URL}/auth/refresh-token`,
            {},
            { withCredentials: true }
        );
        return response.data;
    } catch (error) {
        console.error('Error refreshing token:', error);
        throw error;
    }
};

export const setupTokenRefresh = () => {
    setInterval(async () => {
        try {
            await refreshToken();
        } catch (error) {
            console.error('Failed to refresh token:', error);
            window.location.href = '/login';
        }
    }, 15 * 60 * 1000);
};
