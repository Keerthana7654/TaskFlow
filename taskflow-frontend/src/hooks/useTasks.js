import { useState, useEffect, useCallback } from 'react';
import { taskApi } from '../services/api';
import toast from 'react-hot-toast';

export function useTasks({ status, priority, page = 0, size = 10, isAdmin }) {
  const [tasks, setTasks]       = useState([]);
  const [stats, setStats]       = useState(null);
  const [loading, setLoading]   = useState(true);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  const fetchTasks = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page, size };
      if (status)   params.status   = status;
      if (priority) params.priority = priority;
      const { data } = await taskApi.getAll(params);
      setTasks(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch {
      toast.error('Failed to load tasks');
    } finally {
      setLoading(false);
    }
  }, [page, size, status, priority]);

  const fetchStats = useCallback(async () => {
    try {
      const { data } = await taskApi.getStats();
      setStats(data);
    } catch {}
  }, []);

  useEffect(() => { fetchTasks(); }, [fetchTasks]);
  useEffect(() => { fetchStats(); }, [fetchStats]);

  const refresh = () => { fetchTasks(); fetchStats(); };

  const deleteTask = async (task) => {
    if (!confirm(`Delete "${task.title}"?`)) return false;
    try {
      await taskApi.delete(task.id);
      toast.success('Task deleted');
      refresh();
      return true;
    } catch {
      toast.error('Delete failed');
      return false;
    }
  };

  return { tasks, stats, loading, totalPages, totalElements, refresh, deleteTask };
}
