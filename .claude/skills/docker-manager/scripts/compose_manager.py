#!/usr/bin/env python3
"""
Docker Compose Manager - Handle Docker Compose operations

This script manages Docker Compose files and operations including:
- Service deployment and management
- Configuration validation
- Service scaling
- Environment management
"""

import argparse
import subprocess
import yaml
import json
import sys
import os
from pathlib import Path
from typing import Dict, List, Optional

class DockerComposeManager:
    def __init__(self, compose_file: str = 'docker-compose.yml'):
        self.compose_file = compose_file
        self.compose_available = self._check_compose_availability()
        
    def _check_compose_availability(self) -> bool:
        """Check if Docker Compose is available"""
        try:
            result = subprocess.run(['docker-compose', 'version'], 
                                  capture_output=True, text=True, timeout=10)
            return result.returncode == 0
        except (subprocess.TimeoutExpired, FileNotFoundError):
            # Try docker compose (new syntax)
            try:
                result = subprocess.run(['docker', 'compose', 'version'], 
                                      capture_output=True, text=True, timeout=10)
                return result.returncode == 0
            except (subprocess.TimeoutExpired, FileNotFoundError):
                return False
    
    def _run_compose_command(self, command: List[str], capture_output: bool = True) -> tuple:
        """Execute Docker Compose command with proper encoding handling"""
        if not self.compose_available:
            return 1, "", "Docker Compose is not available"
        
        # Try new docker compose syntax first
        try:
            cmd = ['docker', 'compose'] + command
            if capture_output:
                # Use bytes mode and manual UTF-8 decoding to avoid Windows encoding issues
                result = subprocess.run(cmd, capture_output=True, timeout=120)
                stdout = result.stdout.decode('utf-8', errors='replace') if result.stdout else ""
                stderr = result.stderr.decode('utf-8', errors='replace') if result.stderr else ""
                return result.returncode, stdout, stderr
            else:
                result = subprocess.run(cmd, timeout=120)
                return result.returncode, "", ""
        except Exception as e:
            # Fall back to docker-compose
            try:
                cmd = ['docker-compose'] + command
                if capture_output:
                    # Use bytes mode and manual UTF-8 decoding to avoid Windows encoding issues
                    result = subprocess.run(cmd, capture_output=True, timeout=120)
                    stdout = result.stdout.decode('utf-8', errors='replace') if result.stdout else ""
                    stderr = result.stderr.decode('utf-8', errors='replace') if result.stderr else ""
                    return result.returncode, stdout, stderr
                else:
                    result = subprocess.run(cmd, timeout=120)
                    return result.returncode, "", ""
            except Exception as e2:
                return 1, "", f"Error executing compose command: {str(e2)}"
    
    def validate_config(self) -> Dict:
        """Validate Docker Compose configuration"""
        code, stdout, stderr = self._run_compose_command(['config'])
        if code == 0:
            return {"valid": True, "message": "Configuration is valid", "config": stdout}
        else:
            return {"valid": False, "error": stderr or "Configuration validation failed"}
    
    def list_services(self) -> Dict:
        """List services defined in compose file"""
        code, stdout, stderr = self._run_compose_command(['config', '--services'])
        if code == 0:
            services = stdout.strip().split('\n')
            return {"services": [s for s in services if s]}
        else:
            return {"error": stderr or "Failed to list services"}
    
    def get_service_status(self) -> Dict:
        """Get status of all services"""
        code, stdout, stderr = self._run_compose_command(['ps'])
        if code == 0:
            return {"status": stdout}
        else:
            return {"error": stderr or "Failed to get service status"}
    
    def up_services(self, services: List[str] = None, detach: bool = True, 
                   build: bool = False, force_recreate: bool = False) -> Dict:
        """Start services"""
        cmd = ['up']
        if detach:
            cmd.append('-d')
        if build:
            cmd.append('--build')
        if force_recreate:
            cmd.append('--force-recreate')
        if services:
            cmd.extend(services)
            
        code, stdout, stderr = self._run_compose_command(cmd, capture_output=False)
        if code == 0:
            return {"success": True, "message": "Services started successfully"}
        else:
            return {"success": False, "error": stderr or "Failed to start services"}
    
    def down_services(self, services: List[str] = None, remove_volumes: bool = False,
                     remove_images: str = None) -> Dict:
        """Stop and remove services"""
        cmd = ['down']
        if remove_volumes:
            cmd.append('-v')
        if remove_images:
            cmd.extend(['--rmi', remove_images])
        if services:
            # For specific services, we need to stop them individually
            cmd = ['stop'] + services
            
        code, stdout, stderr = self._run_compose_command(cmd, capture_output=False)
        if code == 0:
            return {"success": True, "message": "Services stopped successfully"}
        else:
            return {"success": False, "error": stderr or "Failed to stop services"}
    
    def restart_services(self, services: List[str] = None) -> Dict:
        """Restart services"""
        cmd = ['restart']
        if services:
            cmd.extend(services)
            
        code, stdout, stderr = self._run_compose_command(cmd, capture_output=False)
        if code == 0:
            return {"success": True, "message": "Services restarted successfully"}
        else:
            return {"success": False, "error": stderr or "Failed to restart services"}
    
    def scale_service(self, service: str, replicas: int) -> Dict:
        """Scale a service to specified number of replicas"""
        code, stdout, stderr = self._run_compose_command(['up', '-d', '--scale', f'{service}={replicas}', service])
        if code == 0:
            return {"success": True, "message": f"Service {service} scaled to {replicas} replicas"}
        else:
            return {"success": False, "error": stderr or "Failed to scale service"}
    
    def get_logs(self, services: List[str] = None, tail: int = 100, 
                follow: bool = False, timestamps: bool = False) -> Dict:
        """Get logs from services"""
        cmd = ['logs']
        if tail > 0:
            cmd.extend(['--tail', str(tail)])
        if follow:
            cmd.append('-f')
        if timestamps:
            cmd.append('-t')
        if services:
            cmd.extend(services)
            
        code, stdout, stderr = self._run_compose_command(cmd)
        if code == 0:
            return {"success": True, "logs": stdout}
        else:
            return {"success": False, "error": stderr or "Failed to get logs"}
    
    def exec_command(self, service: str, command: str) -> Dict:
        """Execute command in a running service container"""
        cmd = ['exec', service] + command.split()
        code, stdout, stderr = self._run_compose_command(cmd)
        if code == 0:
            return {"success": True, "output": stdout}
        else:
            return {"success": False, "error": stderr or "Failed to execute command"}

def main():
    parser = argparse.ArgumentParser(description='Docker Compose Manager')
    parser.add_argument('--action', required=True,
                       choices=['validate', 'list-services', 'status', 'up', 'down', 
                               'restart', 'scale', 'logs', 'exec'],
                       help='Action to perform')
    parser.add_argument('--compose-file', default='docker-compose.yml',
                       help='Docker Compose file path')
    parser.add_argument('--services', nargs='*', help='Service names')
    parser.add_argument('--detach', action='store_true', help='Run in detached mode')
    parser.add_argument('--build', action='store_true', help='Build images before starting')
    parser.add_argument('--force-recreate', action='store_true', help='Recreate containers')
    parser.add_argument('--remove-volumes', action='store_true', help='Remove volumes on down')
    parser.add_argument('--remove-images', choices=['local', 'all'], 
                       help='Remove images on down')
    parser.add_argument('--replicas', type=int, help='Number of replicas for scaling')
    parser.add_argument('--tail', type=int, default=100, help='Number of log lines')
    parser.add_argument('--follow', action='store_true', help='Follow log output')
    parser.add_argument('--timestamps', action='store_true', help='Show timestamps')
    parser.add_argument('--command', help='Command to execute in service')
    
    args = parser.parse_args()
    
    manager = DockerComposeManager(args.compose_file)
    
    if args.action == 'validate':
        result = manager.validate_config()
    elif args.action == 'list-services':
        result = manager.list_services()
    elif args.action == 'status':
        result = manager.get_service_status()
    elif args.action == 'up':
        result = manager.up_services(args.services, args.detach, args.build, args.force_recreate)
    elif args.action == 'down':
        result = manager.down_services(args.services, args.remove_volumes, args.remove_images)
    elif args.action == 'restart':
        result = manager.restart_services(args.services)
    elif args.action == 'scale' and args.services and args.replicas:
        result = manager.scale_service(args.services[0], args.replicas)
    elif args.action == 'logs':
        result = manager.get_logs(args.services, args.tail, args.follow, args.timestamps)
    elif args.action == 'exec' and args.services and args.command:
        result = manager.exec_command(args.services[0], args.command)
    else:
        result = {"error": "Invalid arguments or missing required parameters"}
    
    print(json.dumps(result, indent=2))

if __name__ == '__main__':
    main()