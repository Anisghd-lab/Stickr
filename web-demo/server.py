#!/usr/bin/env python3
import http.server
import socketserver
import os
import sys

PORT = 8080
DIRECTORY = os.path.dirname(os.path.abspath(__file__))

class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIRECTORY, **kwargs)

    def end_headers(self):
        # Permettre l'accès CORS et désactiver le cache pour la démo
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Cache-Control', 'no-cache, no-store, must-revalidate')
        super().end_headers()

def run():
    Handler.extensions_map.update({
        '.webp': 'image/webp',
        '.js': 'application/javascript',
        '.css': 'text/css',
        '.html': 'text/html',
        '.json': 'application/json',
        '.svg': 'image/svg+xml'
    })

    # Permettre la réutilisation immédiate du port
    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("0.0.0.0", PORT), Handler) as httpd:
        print(f"============================================================")
        print(f"🚀 Serveur Web Stickr actif sur : http://localhost:{PORT}")
        print(f"📁 Racine : {DIRECTORY}")
        print(f"============================================================")
        sys.stdout.flush()
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nArrêt du serveur.")
            httpd.server_close()

if __name__ == '__main__':
    run()
