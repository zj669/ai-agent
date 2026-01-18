#!/usr/bin/env python3
"""
Docker Manager - Comprehensive Docker operations toolkit

This script provides unified interface for Docker container management,
including deployment, monitoring, error analysis, and compose operations.

Features:
- Container lifecycle management (start, stop, restart, remove)
- Image management (build, pull, push, list)
- Log analysis and error detection
- Container status monitoring
- Docker Compose integration
"""

import argparse
import subprocess
import json
import sys
import os
import re
from datetime import datetime
from typing import Dict, List, Optional, Tuple
from pathlib import Path

class DockerManager:
    def __init__(self):
        self.docker_available = self._check_docker_availability()
        
    def _check_docker_availability(self) -> bool:
        """Check if Docker is available and running"""
        try:
            result = subprocess.run(['docker', 'version'], 
                                  capture_output=True, text=True, timeout=10)
            return result.returncode == 0
        except (subprocess.TimeoutExpired, FileNotFoundError):
            return False
    
    def _run_docker_command(self, command: List[str], capture_output: bool = True) -> Tuple[int, str, str]:
        """Execute Docker command with proper error handling and encoding"""
        if not self.docker_available:
            return 1, "", "Docker is not available or not running"
            
        try:
            if capture_output:
                # Use bytes mode and manual UTF-8 decoding to avoid Windows encoding issues
                result = subprocess.run(['docker'] + command, 
                                      capture_output=True, timeout=60)
                stdout = result.stdout.decode('utf-8', errors='replace') if result.stdout else ""
                stderr = result.stderr.decode('utf-8', errors='replace') if result.stderr else ""
                return result.returncode, stdout, stderr
            else:
                result = subprocess.run(['docker'] + command, timeout=60)
                return result.returncode, "", ""
        except subprocess.TimeoutExpired:
            return 1, "", f"Command timed out: docker {' '.join(command)}"
        except Exception as e:
            return 1, "", f"Error executing command: {str(e)}"
    
    def list_containers(self, all_containers: bool = False) -> Dict:
        """List Docker containers"""
        cmd = ['ps']
        if all_containers:
            cmd.append('-a')
        cmd.extend(['--format', 'json'])
        
        # Fallback to table format if json not supported
        code, stdout, stderr = self._run_docker_command(cmd)
        if code != 0:
            # Try table format
            cmd = ['ps']
            if all_containers:
                cmd.append('-a')
            code, stdout, stderr = self._run_docker_command(cmd)
            if code == 0:
                return {"containers": self._parse_ps_table(stdout), "format": "table"}
            else:
                return {"error": stderr or "Failed to list containers"}
        
        try:
            containers = [json.loads(line) for line in stdout.strip().split('\n') if line]
            return {"containers": containers, "format": "json"}
        except json.JSONDecodeError:
            return {"containers": [], "format": "raw", "raw_output": stdout}
    
    def _parse_ps_table(self, output: str) -> List[Dict]:
        """Parse docker ps table output"""
        lines = output.strip().split('\n')
        if len(lines) < 2:
            return []
        
        # Parse header and data rows
        header = lines[0].split()
        containers = []
        
        for line in lines[1:]:
            if line.strip():
                parts = line.split()
                if len(parts) >= len(header):
                    container_info = {}
                    for i, key in enumerate(header[:len(parts)]):
                        container_info[key.lower()] = parts[i]
                    containers.append(container_info)
        
        return containers
    
    def start_container(self, container_name: str) -> Dict:
        """Start a Docker container"""
        code, stdout, stderr = self._run_docker_command(['start', container_name])
        if code == 0:
            return {"success": True, "message": f"Container {container_name} started successfully"}
        else:
            return {"success": False, "error": stderr or "Failed to start container"}
    
    def stop_container(self, container_name: str, timeout: int = 10) -> Dict:
        """Stop a Docker container"""
        cmd = ['stop', '-t', str(timeout), container_name]
        code, stdout, stderr = self._run_docker_command(cmd)
        if code == 0:
            return {"success": True, "message": f"Container {container_name} stopped successfully"}
        else:
            return {"success": False, "error": stderr or "Failed to stop container"}
    
    def restart_container(self, container_name: str, timeout: int = 10) -> Dict:
        """Restart a Docker container"""
        cmd = ['restart', '-t', str(timeout), container_name]
        code, stdout, stderr = self._run_docker_command(cmd)
        if code == 0:
            return {"success": True, "message": f"Container {container_name} restarted successfully"}
        else:
            return {"success": False, "error": stderr or "Failed to restart container"}
    
    def remove_container(self, container_name: str, force: bool = False) -> Dict:
        """Remove a Docker container"""
        cmd = ['rm']
        if force:
            cmd.append('-f')
        cmd.append(container_name)
        code, stdout, stderr = self._run_docker_command(cmd)
        if code == 0:
            return {"success": True, "message": f"Container {container_name} removed successfully"}
        else:
            return {"success": False, "error": stderr or "Failed to remove container"}
    
    def get_container_logs(self, container_name: str, tail: int = 100, 
                          follow: bool = False, timestamps: bool = False) -> Dict:
        """Get container logs"""
        cmd = ['logs']
        if tail > 0:
            cmd.extend(['--tail', str(tail)])
        if follow:
            cmd.append('-f')
        if timestamps:
            cmd.append('-t')
        cmd.append(container_name)
        
        code, stdout, stderr = self._run_docker_command(cmd)
        if code == 0:
            return {"success": True, "logs": stdout}
        else:
            return {"success": False, "error": stderr or "Failed to get container logs"}
    
    def analyze_logs_for_errors(self, logs: str) -> Dict:
        """Analyze logs for common error patterns"""
        error_patterns = [
            (r'ERROR.*', 'General Error'),
            (r'Exception.*', 'Exception'),
            (r'(connection|connect).*refused', 'Connection Refused'),
            (r'(connection|connect).*timeout', 'Connection Timeout'),
            (r'memory.*exceeded', 'Memory Issue'),
            (r'permission.*denied', 'Permission Denied'),
            (r'file.*not found', 'File Not Found'),
            (r'port.*already in use', 'Port Conflict'),
        ]
        
        errors_found = []
        lines = logs.split('\n')
        
        for line_num, line in enumerate(lines, 1):
            for pattern, error_type in error_patterns:
                if re.search(pattern, line, re.IGNORECASE):
                    errors_found.append({
                        'line': line_num,
                        'content': line.strip(),
                        'type': error_type,
                        'pattern': pattern
                    })
        
        return {
            'total_lines': len(lines),
            'errors_found': len(errors_found),
            'error_details': errors_found,
            'summary': f"Found {len(errors_found)} potential errors in {len(lines)} log lines"
        }
    
    def inspect_container(self, container_name: str) -> Dict:
        """Get detailed container information"""
        code, stdout, stderr = self._run_docker_command(['inspect', container_name])
        if code == 0:
            try:
                data = json.loads(stdout)
                return {"success": True, "inspection": data}
            except json.JSONDecodeError:
                return {"success": False, "error": "Invalid JSON response from Docker"}
        else:
            return {"success": False, "error": stderr or "Failed to inspect container"}

def main():
    parser = argparse.ArgumentParser(description='Docker Manager Toolkit')
    parser.add_argument('--action', required=True, 
                       choices=['list', 'start', 'stop', 'restart', 'remove', 'logs', 'inspect'],
                       help='Action to perform')
    parser.add_argument('--container', help='Container name or ID')
    parser.add_argument('--all', action='store_true', help='Show all containers (for list)')
    parser.add_argument('--tail', type=int, default=100, help='Number of log lines to show')
    parser.add_argument('--follow', action='store_true', help='Follow log output')
    parser.add_argument('--force', action='store_true', help='Force operation')
    parser.add_argument('--timeout', type=int, default=10, help='Timeout for stop/restart')
    parser.add_argument('--timestamps', action='store_true', help='Show timestamps in logs')
    parser.add_argument('--analyze', action='store_true', help='Analyze logs for errors')
    
    args = parser.parse_args()
    
    manager = DockerManager()
    
    if args.action == 'list':
        result = manager.list_containers(all_containers=args.all)
    elif args.action == 'start' and args.container:
        result = manager.start_container(args.container)
    elif args.action == 'stop' and args.container:
        result = manager.stop_container(args.container, args.timeout)
    elif args.action == 'restart' and args.container:
        result = manager.restart_container(args.container, args.timeout)
    elif args.action == 'remove' and args.container:
        result = manager.remove_container(args.container, args.force)
    elif args.action == 'logs' and args.container:
        result = manager.get_container_logs(args.container, args.tail, args.follow, args.timestamps)
        if args.analyze and result.get('success'):
            analysis = manager.analyze_logs_for_errors(result['logs'])
            result['analysis'] = analysis
    elif args.action == 'inspect' and args.container:
        result = manager.inspect_container(args.container)
    else:
        result = {"error": "Invalid arguments or missing required parameters"}
    
    print(json.dumps(result, indent=2))

if __name__ == '__main__':
    main()