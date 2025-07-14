FROM python:3.11-slim

RUN apt-get update && apt-get install -y gcc g++ git curl && rm -rf /var/lib/apt/lists/*

RUN pip install --no-cache-dir mcp-server-fetch mcp-proxy

EXPOSE 8080

ENTRYPOINT ["mcp-proxy", "--host=0.0.0.0", "--port=8080", "--"]
CMD ["python", "-m", "mcp_server_fetch"]
