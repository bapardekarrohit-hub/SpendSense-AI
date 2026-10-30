import unittest
try:
    from fastapi.testclient import TestClient
    from app.main import app
    HAVE = True
except ImportError:
    HAVE = False


@unittest.skipUnless(HAVE, "fastapi/httpx not installed")
class ApiTests(unittest.TestCase):
    def setUp(self):
        self.client = TestClient(app)

    def test_health(self):
        self.assertEqual(self.client.get("/health").json(), {"status": "ok"})

    def test_rejects_negative_amount(self):
        body = {"transactions": [{"title": "x", "amount": -1, "type": "expense",
                                  "category": "Food", "date": "2026-10-01"}]}
        self.assertEqual(self.client.post("/analyze", json=body).status_code, 422)

    def test_analyze_ok(self):
        tx = [{"title": "Meals", "amount": 100 + i, "type": "expense", "category": "Food",
               "date": f"2026-09-{(i % 28) + 1:02d}"} for i in range(40)]
        r = self.client.post("/analyze", json={"transactions": tx})
        self.assertEqual(r.status_code, 200)
        self.assertIn("forecast", r.json())


if __name__ == "__main__":
    unittest.main()
