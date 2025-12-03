#!/usr/bin/env python3
"""
Herramienta de diagnóstico para detectar problemas de conectividad
entre celular y servidor en VPN/WiFi (sin dependencias externas)
"""
import socket
import subprocess
import json
import platform
import time
import re

def obtener_interfaces_red():
    """Obtiene interfaces de red usando comandos del sistema"""
    interfaces = {}
    try:
        if platform.system().lower() == 'windows':
            # Usar ipconfig en Windows
            result = subprocess.run(['ipconfig'], capture_output=True, text=True)
            output = result.stdout

            # Parsear salida de ipconfig
            current_adapter = None
            for line in output.split('\n'):
                line = line.strip()
                if 'adaptador' in line.lower() or 'adapter' in line.lower():
                    current_adapter = line
                elif 'IPv4' in line and current_adapter:
                    ip_match = re.search(r'(\d+\.\d+\.\d+\.\d+)', line)
                    if ip_match:
                        ip = ip_match.group(1)
                        if ip != '127.0.0.1':
                            interfaces[current_adapter] = {
                                'ip': ip,
                                'tipo': detectar_tipo_ip(ip)
                            }
        else:
            # Para Linux/Mac
            result = subprocess.run(['ip', 'addr'], capture_output=True, text=True)
            # Parsear salida de ip addr (implementación básica)
            pass

    except Exception as e:
        print(f"❌ Error obteniendo interfaces: {e}")
        # Fallback: obtener IP local básica
        try:
            hostname = socket.gethostname()
            local_ip = socket.gethostbyname(hostname)
            interfaces['Local'] = {
                'ip': local_ip,
                'tipo': detectar_tipo_ip(local_ip)
            }
        except:
            pass

    return interfaces

def detectar_tipo_ip(ip):
    """Detecta el tipo de conexión basado en la IP"""
    if ip.startswith('192.168.'):
        return "WiFi Local"
    elif ip.startswith('10.0.11.'):
        return "VPN"
    elif ip.startswith('10.'):
        return "Red Interna"
    elif ip.startswith('172.'):
        return "Red Privada"
    elif ip.startswith('26.'):
        return "Radmin VPN"
    else:
        return "Desconocido"

def obtener_ips_disponibles():
    """Obtiene todas las IPs disponibles del sistema"""
    ips = []
    try:
        # Método 1: Conectar a una dirección externa para obtener IP local
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip_local = s.getsockname()[0]
        s.close()
        ips.append({
            'ip': ip_local,
            'tipo': detectar_tipo_ip(ip_local),
            'interface': 'Principal'
        })

        # Método 2: Usar hostname
        hostname = socket.gethostname()
        ip_hostname = socket.gethostbyname(hostname)
        if ip_hostname != ip_local:
            ips.append({
                'ip': ip_hostname,
                'tipo': detectar_tipo_ip(ip_hostname),
                'interface': 'Hostname'
            })

    except Exception as e:
        print(f"❌ Error obteniendo IPs: {e}")

    return ips

def verificar_puerto_abierto(ip, puerto):
    """Verifica si un puerto está abierto en una IP específica"""
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(3)
        resultado = sock.connect_ex((ip, puerto))
        sock.close()
        return resultado == 0
    except:
        return False

def verificar_firewall():
    """Verifica reglas del firewall de Windows"""
    try:
        cmd = 'netsh advfirewall firewall show rule name="Python 8090" dir=in'
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
        return "Python 8090" in result.stdout
    except:
        return False

def crear_regla_firewall():
    """Crea regla de firewall para el puerto 8090"""
    try:
        cmd = 'netsh advfirewall firewall add rule name="Python 8090" dir=in action=allow protocol=TCP localport=8090'
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
        return result.returncode == 0
    except:
        return False

def hacer_ping(ip):
    """Hace ping a una IP"""
    try:
        param = '-n' if platform.system().lower() == 'windows' else '-c'
        cmd = ['ping', param, '1', ip]
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=5)
        return result.returncode == 0
    except:
        return False

def main():
    print("🔍 DIAGNÓSTICO DE CONECTIVIDAD CELULAR-SERVIDOR")
    print("=" * 60)

    # 1. Obtener IPs disponibles
    print("\n1️⃣ IPs DISPONIBLES EN TU PC:")
    ips_disponibles = obtener_ips_disponibles()
    for ip_info in ips_disponibles:
        print(f"   📡 {ip_info['interface']}: {ip_info['ip']} ({ip_info['tipo']})")

    # 2. Verificar interfaces adicionales con ipconfig
    print("\n2️⃣ INTERFACES DE RED COMPLETAS:")
    interfaces = obtener_interfaces_red()
    for nombre, info in interfaces.items():
        print(f"   🔗 {nombre}: {info['ip']} ({info['tipo']})")

    # 3. Verificar firewall
    print("\n3️⃣ VERIFICACIÓN DE FIREWALL:")
    if verificar_firewall():
        print("   ✅ Regla de firewall encontrada")
    else:
        print("   ❌ Regla de firewall NO encontrada")
        print("   🔧 Intentando crear regla...")
        if crear_regla_firewall():
            print("   ✅ Regla de firewall creada exitosamente")
        else:
            print("   ❌ Error creando regla (ejecutar como administrador)")

    # 4. Verificar puertos
    print("\n4️⃣ VERIFICACIÓN DE PUERTOS:")
    todas_ips = ips_disponibles
    for ip_info in todas_ips:
        ip = ip_info['ip']
        if verificar_puerto_abierto(ip, 8090):
            print(f"   ✅ Puerto 8090 ABIERTO en {ip}")
        else:
            print(f"   ❌ Puerto 8090 CERRADO en {ip}")

    # 5. Generar URLs para probar
    print("\n5️⃣ URLs PARA PROBAR EN EL CELULAR:")
    for ip_info in todas_ips:
        url = f"http://{ip_info['ip']}:8090"
        print(f"   🌐 {ip_info['tipo']}: {url}")

    # 6. Diagnóstico específico para tu problema
    print("\n6️⃣ DIAGNÓSTICO ESPECÍFICO:")
    vpn_encontrada = any(ip['tipo'] == 'VPN' for ip in todas_ips)
    wifi_encontrada = any(ip['tipo'] == 'WiFi Local' for ip in todas_ips)

    if vpn_encontrada and wifi_encontrada:
        print("   ✅ VPN y WiFi detectadas - Problema puede ser de firewall o configuración")
    elif vpn_encontrada:
        print("   🔒 Solo VPN detectada - Asegúrate de que el celular esté en la misma VPN")
    elif wifi_encontrada:
        print("   📶 Solo WiFi detectada - Verifica que el celular esté en la misma red")
    else:
        print("   ❌ No se detectaron conexiones típicas")

    # 7. Recomendaciones
    print("\n7️⃣ RECOMENDACIONES PARA SOLUCIONAR:")
    print("   🔧 Ejecuta como administrador para configurar firewall")
    print("   🔄 Reinicia el servidor después de cambios")
    print("   📱 Verifica que el celular esté en la misma red/VPN")
    print("   🔥 Desactiva temporalmente Windows Defender Firewall")
    print("   🌐 Usa el servidor-diagnostico.py para mejor detección")

    # 8. Comando para ejecutar servidor
    print("\n8️⃣ PRÓXIMOS PASOS:")
    print("   python servidor-diagnostico.py")
    print("   (El servidor mejorado configurará automáticamente el firewall)")

    print("\n" + "=" * 60)
    print("✅ Diagnóstico completado")

if __name__ == "__main__":
    main()
