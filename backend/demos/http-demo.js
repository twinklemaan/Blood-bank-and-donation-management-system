// demos/http-demo.js - Pure Node.js Server Demo (Unit 3: HTTP, URL, and File System modules)
// Demonstrates low-level request routing and query handling without Express.
const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3000;
const dataFilePath = path.join(__dirname, 'demo-data.json');

const server = http.createServer((req, res) => {
  // SYLLABUS CONCEPT: URL Module - parse incoming URL and search query params
  const parsedUrl = new URL(req.url, `http://${req.headers.host}`);
  const pathname = parsedUrl.pathname;

  // Route 1: Root endpoint returning plain text
  if (req.method === 'GET' && pathname === '/') {
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    return res.end('Blood Bank Node.js HTTP/URL/FS Core Demo Server');
  }

  // Route 2: /stock query endpoint using fs.readFile and URL query filtering
  if (req.method === 'GET' && pathname === '/stock') {
    const requestedGroup = parsedUrl.searchParams.get('group');

    // SYLLABUS CONCEPT: File System Module - asynchronous file reading
    return fs.readFile(dataFilePath, 'utf8', (err, rawData) => {
      if (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ error: 'Failed to read demo data file' }));
      }

      let stock = JSON.parse(rawData);
      if (requestedGroup) {
        stock = stock.filter((item) => item.bloodGroup.toUpperCase() === requestedGroup.toUpperCase());
      }

      res.writeHead(200, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ query: requestedGroup || 'ALL', count: stock.length, data: stock }));
    });
  }

  // Fallback 404 handler
  res.writeHead(404, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ message: 'Route not found in core demo' }));
});

server.listen(PORT, () => {
  console.log(`Core Node.js Demo Server running on http://localhost:${PORT}`);
  console.log(`Try GET http://localhost:${PORT}/ or http://localhost:${PORT}/stock?group=O%2B`);
});

module.exports = server;
