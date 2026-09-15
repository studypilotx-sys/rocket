import React, { useState, useRef } from 'react';
import {
  Plus,
  Search,
  ChevronLeft,
  FileText,
  Video,
  Image as ImageIcon,
  FileCheck,
  ShieldCheck,
  Eye,
  Trash2,
  Edit3,
  MoreVertical,
  X,
  Upload,
  Calendar,
  BookOpen,
  Folder,
  Check,
  Clock,
  Play,
  Download,
  AlertCircle
} from 'lucide-react';
import { StudyMaterialItem, Subject, Chapter, Topic, MaterialCategoryFilter, MaterialType } from '../types';

interface StudyMaterialsViewProps {
  materials: StudyMaterialItem[];
  subjects: Subject[];
  chapters: Chapter[];
  topics: Topic[];
  onAddMaterial: (material: StudyMaterialItem) => void;
  onUpdateMaterial: (material: StudyMaterialItem) => void;
  onDeleteMaterial: (materialId: string) => void;
  onStudyWithGuardian: (material: StudyMaterialItem) => void;
  onBack: () => void;
}

export const StudyMaterialsView: React.FC<StudyMaterialsViewProps> = ({
  materials,
  subjects,
  chapters,
  topics,
  onAddMaterial,
  onUpdateMaterial,
  onDeleteMaterial,
  onStudyWithGuardian,
  onBack,
}) => {
  const [categoryFilter, setCategoryFilter] = useState<MaterialCategoryFilter>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [activeViewerMaterial, setActiveViewerMaterial] = useState<StudyMaterialItem | null>(null);

  // Add / Edit Modal State
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [editingMaterial, setEditingMaterial] = useState<StudyMaterialItem | null>(null);

  // Form State
  const [formName, setFormName] = useState('');
  const [formType, setFormType] = useState<MaterialType>('PDF');
  const [formSubjectId, setFormSubjectId] = useState(subjects[0]?.id || '');
  const [formChapterId, setFormChapterId] = useState('');
  const [formTopicId, setFormTopicId] = useState('');
  const [formNotes, setFormNotes] = useState('');
  const [formUri, setFormUri] = useState('');
  const [formFileSize, setFormFileSize] = useState('');
  const [fileError, setFileError] = useState('');

  // Dropdown menus
  const [activeMenuId, setActiveMenuId] = useState<string | null>(null);
  const [materialToDelete, setMaterialToDelete] = useState<StudyMaterialItem | null>(null);

  const fileInputRef = useRef<HTMLInputElement | null>(null);

  // Filter available chapters based on selected subject
  const availableChapters = chapters.filter(c => c.subjectId === formSubjectId);
  // Filter available topics based on selected chapter
  const availableTopics = topics.filter(t => t.chapterId === formChapterId);

  // Handle opening the Add Modal
  const handleOpenAddModal = () => {
    const defaultSubject = subjects[0]?.id || '';
    const defChapters = chapters.filter(c => c.subjectId === defaultSubject);
    const defaultChapter = defChapters[0]?.id || '';
    const defTopics = topics.filter(t => t.chapterId === defaultChapter);

    setFormName('');
    setFormType('PDF');
    setFormSubjectId(defaultSubject);
    setFormChapterId(defaultChapter);
    setFormTopicId(defTopics[0]?.id || '');
    setFormNotes('');
    setFormUri('');
    setFormFileSize('');
    setFileError('');
    setEditingMaterial(null);
    setIsAddModalOpen(true);
  };

  // Handle opening the Edit Modal
  const handleOpenEditModal = (material: StudyMaterialItem) => {
    setEditingMaterial(material);
    setFormName(material.name);
    setFormType(material.type);
    setFormSubjectId(material.subjectId);
    setFormChapterId(material.chapterId);
    setFormTopicId(material.topicId || '');
    setFormNotes(material.notes || '');
    setFormUri(material.uriOrPath);
    setFormFileSize(material.fileSize || '');
    setFileError('');
    setActiveMenuId(null);
    setIsAddModalOpen(true);
  };

  // Handle native file selection
  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Check 25MB file limit
    if (file.size > 25 * 1024 * 1024) {
      setFileError('File exceeds 25 MB limit for local study materials. Please select a smaller file or notes.');
      return;
    }
    setFileError('');

    // Detect type
    let detectedType: MaterialType = 'DOCUMENT';
    if (file.type.startsWith('video/') || /\.(mp4|webm|mkv|mov|avi)$/i.test(file.name)) {
      detectedType = 'VIDEO';
    } else if (file.type === 'application/pdf' || /\.pdf$/i.test(file.name)) {
      detectedType = 'PDF';
    } else if (file.type.startsWith('image/') || /\.(png|jpe?g|webp|gif|svg)$/i.test(file.name)) {
      detectedType = 'IMAGE';
    } else if (file.type.startsWith('text/') || /\.(txt|md|notes|rtf)$/i.test(file.name)) {
      detectedType = 'NOTES';
    }

    setFormType(detectedType);
    if (!formName) {
      setFormName(file.name);
    }

    // Format file size
    const sizeInKB = file.size / 1024;
    const formattedSize = sizeInKB > 1024
      ? `${(sizeInKB / 1024).toFixed(1)} MB`
      : `${Math.round(sizeInKB)} KB`;
    setFormFileSize(formattedSize);

    // Read file: For video, create object URL for high performance and zero storage footprint
    if (detectedType === 'VIDEO') {
      try {
        const objectUrl = URL.createObjectURL(file);
        setFormUri(objectUrl);
      } catch {
        const reader = new FileReader();
        reader.onload = () => setFormUri(reader.result as string || '');
        reader.readAsDataURL(file);
      }
    } else if (detectedType === 'NOTES') {
      const reader = new FileReader();
      reader.onload = () => {
        setFormUri(reader.result as string || '');
      };
      reader.readAsText(file);
    } else {
      const reader = new FileReader();
      reader.onload = () => {
        setFormUri(reader.result as string || '');
      };
      reader.readAsDataURL(file);
    }
  };

  // Handle Form Submit
  const handleSaveMaterial = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formName.trim()) {
      setFileError('Please enter a name for the study material');
      return;
    }

    if (!formSubjectId || !formChapterId) {
      setFileError('Please associate the material with a subject and chapter');
      return;
    }

    // Provide placeholder URI if no file was uploaded
    let finalUri = formUri;
    if (!finalUri) {
      if (formType === 'VIDEO') {
        finalUri = 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4';
      } else if (formType === 'IMAGE') {
        finalUri = 'https://images.unsplash.com/photo-1532094349884-543bc11b234d?auto=format&fit=crop&w=800&q=80';
      } else {
        finalUri = formNotes || 'Local study reference notes for curriculum topic.';
      }
    }

    if (editingMaterial) {
      const updated: StudyMaterialItem = {
        ...editingMaterial,
        name: formName.trim(),
        type: formType,
        subjectId: formSubjectId,
        chapterId: formChapterId,
        topicId: formTopicId || undefined,
        notes: formNotes.trim() || undefined,
        uriOrPath: finalUri,
        fileSize: formFileSize || editingMaterial.fileSize || '1.2 MB',
      };
      onUpdateMaterial(updated);
    } else {
      const newMaterial: StudyMaterialItem = {
        id: `mat-${Date.now()}`,
        name: formName.trim(),
        type: formType,
        uriOrPath: finalUri,
        fileSize: formFileSize || '1.5 MB',
        subjectId: formSubjectId,
        chapterId: formChapterId,
        topicId: formTopicId || undefined,
        dateAdded: Date.now(),
        notes: formNotes.trim() || undefined,
      };
      onAddMaterial(newMaterial);
    }

    setIsAddModalOpen(false);
  };

  // Filter materials based on category and search
  const filteredMaterials = materials.filter(mat => {
    // Category match
    if (categoryFilter === 'VIDEOS' && mat.type !== 'VIDEO') return false;
    if (categoryFilter === 'PDFS' && mat.type !== 'PDF') return false;
    if (categoryFilter === 'NOTES' && mat.type !== 'NOTES') return false;
    if (categoryFilter === 'IMAGES' && mat.type !== 'IMAGE') return false;
    if (categoryFilter === 'DOCUMENTS' && mat.type !== 'DOCUMENT') return false;

    // Search query match
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      const subject = subjects.find(s => s.id === mat.subjectId)?.name.toLowerCase() || '';
      const chapter = chapters.find(c => c.id === mat.chapterId)?.name.toLowerCase() || '';
      const topic = topics.find(t => t.id === mat.topicId)?.name.toLowerCase() || '';
      return (
        mat.name.toLowerCase().includes(q) ||
        subject.includes(q) ||
        chapter.includes(q) ||
        topic.includes(q)
      );
    }

    return true;
  });

  // Type helper icon and color
  const getTypeBadge = (type: MaterialType) => {
    switch (type) {
      case 'VIDEO':
        return { icon: Video, color: 'bg-[#FFF3E0] text-[#E65100] border-[#FFE0B2]', label: 'Video' };
      case 'PDF':
        return { icon: FileText, color: 'bg-[#FFEBEE] text-[#C62828] border-[#FFCDD2]', label: 'PDF' };
      case 'NOTES':
        return { icon: Edit3, color: 'bg-[#E8F5E9] text-[#2E7D32] border-[#C8E6C9]', label: 'Notes' };
      case 'IMAGE':
        return { icon: ImageIcon, color: 'bg-[#EDE7F6] text-[#6A1B9A] border-[#D1C4E9]', label: 'Image' };
      case 'DOCUMENT':
      default:
        return { icon: FileCheck, color: 'bg-[#E3F2FD] text-[#1565C0] border-[#BBDEFB]', label: 'Doc' };
    }
  };

  return (
    <div className="p-5 space-y-4 pb-12">
      {/* Top Bar */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-2">
          <button
            id="materials-back-btn"
            onClick={onBack}
            className="p-1.5 rounded-lg hover:bg-[#F1F5F9] text-[#0F172A] transition-colors"
            aria-label="Back to Dashboard"
          >
            <ChevronLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="text-lg font-bold text-[#0F172A]">Study Materials</h1>
            <p className="text-[11px] text-[#64748B]">{materials.length} local curriculum files</p>
          </div>
        </div>

        <button
          id="add-material-btn"
          onClick={handleOpenAddModal}
          className="bg-[#B8860B] hover:bg-[#A17608] text-white px-3 py-2 rounded-xl text-xs font-bold flex items-center space-x-1.5 transition-colors shadow-xs"
        >
          <Plus className="w-4 h-4" />
          <span>Add Material</span>
        </button>
      </div>

      {/* Search Input */}
      <div className="relative">
        <Search className="w-4 h-4 text-[#64748B] absolute left-3.5 top-3" />
        <input
          id="materials-search-input"
          type="text"
          value={searchQuery}
          onChange={e => setSearchQuery(e.target.value)}
          placeholder="Search materials, subjects, chapters..."
          className="w-full pl-10 pr-9 py-2.5 bg-white border border-[#E2E8F0] rounded-xl text-xs text-[#0F172A] placeholder-[#94A3B8] focus:outline-none focus:border-[#B8860B] shadow-2xs"
        />
        {searchQuery && (
          <button
            onClick={() => setSearchQuery('')}
            className="absolute right-3 top-3 text-[#64748B] hover:text-[#0F172A]"
          >
            <X className="w-4 h-4" />
          </button>
        )}
      </div>

      {/* Category Filter Chips */}
      <div className="flex items-center space-x-1.5 overflow-x-auto pb-1 no-scrollbar">
        {[
          { id: 'ALL', label: 'All', count: materials.length },
          { id: 'VIDEOS', label: 'Videos', count: materials.filter(m => m.type === 'VIDEO').length },
          { id: 'PDFS', label: 'PDFs', count: materials.filter(m => m.type === 'PDF').length },
          { id: 'NOTES', label: 'Notes', count: materials.filter(m => m.type === 'NOTES').length },
          { id: 'IMAGES', label: 'Images', count: materials.filter(m => m.type === 'IMAGE').length },
          { id: 'DOCUMENTS', label: 'Docs', count: materials.filter(m => m.type === 'DOCUMENT').length },
        ].map(cat => (
          <button
            key={cat.id}
            onClick={() => setCategoryFilter(cat.id as MaterialCategoryFilter)}
            className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap border transition-all ${
              categoryFilter === cat.id
                ? 'bg-[#B8860B] text-white border-[#B8860B] shadow-2xs'
                : 'bg-white text-[#64748B] border-[#E2E8F0] hover:bg-[#F8FAFC]'
            }`}
          >
            {cat.label} ({cat.count})
          </button>
        ))}
      </div>

      {/* Materials List */}
      {filteredMaterials.length === 0 ? (
        <div className="bg-white rounded-2xl border border-[#E2E8F0] p-8 text-center space-y-3 shadow-xs">
          <div className="w-12 h-12 rounded-xl bg-[#FEFCE8] border border-[#FEF08A] flex items-center justify-center text-[#B8860B] mx-auto shadow-2xs">
            <Folder className="w-6 h-6" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-[#0F172A]">No Study Materials Found</h3>
            <p className="text-xs text-[#64748B] mt-1 max-w-xs mx-auto">
              {searchQuery
                ? 'No items matched your query. Try clearing the search filter.'
                : 'Add video lectures, textbook PDFs, handwritten notes, or diagrams linked to your subjects.'}
            </p>
          </div>
          <button
            onClick={handleOpenAddModal}
            className="inline-flex items-center space-x-1.5 px-4 py-2 bg-[#F8FAFC] hover:bg-[#F1F5F9] text-[#0F172A] border border-[#E2E8F0] rounded-xl text-xs font-bold transition-colors"
          >
            <Plus className="w-4 h-4 text-[#B8860B]" />
            <span>Import First Material</span>
          </button>
        </div>
      ) : (
        <div className="space-y-3">
          {filteredMaterials.map(item => {
            const subject = subjects.find(s => s.id === item.subjectId);
            const chapter = chapters.find(c => c.id === item.chapterId);
            const topic = topics.find(t => t.id === item.topicId);
            const badge = getTypeBadge(item.type);
            const Icon = badge.icon;

            return (
              <div
                key={item.id}
                className="bg-white rounded-2xl border border-[#E2E8F0] p-4 space-y-3 shadow-2xs hover:border-[#CBD5E1] transition-all relative"
              >
                {/* Top Row: Type badge, name, menu */}
                <div className="flex items-start justify-between">
                  <div className="flex items-start space-x-3 flex-1 min-w-0 pr-2">
                    <div className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 border ${badge.color}`}>
                      <Icon className="w-4 h-4" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <h3 className="text-xs font-bold text-[#0F172A] truncate" title={item.name}>
                        {item.name}
                      </h3>
                      <div className="flex items-center space-x-1.5 text-[10px] text-[#64748B] mt-0.5">
                        <span className="font-semibold text-[#B8860B] truncate">{subject?.name || 'General'}</span>
                        <span>•</span>
                        <span className="truncate">{chapter?.name || 'Curriculum'}</span>
                        {item.fileSize && (
                          <>
                            <span>•</span>
                            <span className="font-mono">{item.fileSize}</span>
                          </>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* 3-dot Menu */}
                  <div className="relative">
                    <button
                      onClick={() => setActiveMenuId(activeMenuId === item.id ? null : item.id)}
                      className="p-1 rounded-lg text-[#64748B] hover:bg-[#F1F5F9] hover:text-[#0F172A]"
                      aria-label="Options"
                    >
                      <MoreVertical className="w-4 h-4" />
                    </button>

                    {activeMenuId === item.id && (
                      <>
                        <div className="fixed inset-0 z-20" onClick={() => setActiveMenuId(null)} />
                        <div className="absolute right-0 top-7 bg-white rounded-xl border border-[#E2E8F0] shadow-lg py-1 w-36 z-30">
                          <button
                            onClick={() => handleOpenEditModal(item)}
                            className="w-full px-3 py-1.5 text-left text-xs font-medium text-[#0F172A] hover:bg-[#F8FAFC] flex items-center space-x-2"
                          >
                            <Edit3 className="w-3.5 h-3.5 text-[#B8860B]" />
                            <span>Rename / Edit</span>
                          </button>
                          <button
                            type="button"
                            onClick={(e) => {
                              e.stopPropagation();
                              setMaterialToDelete(item);
                              setActiveMenuId(null);
                            }}
                            className="w-full px-3 py-1.5 text-left text-xs font-medium text-red-600 hover:bg-red-50 flex items-center space-x-2"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                            <span>Delete</span>
                          </button>
                        </div>
                      </>
                    )}
                  </div>
                </div>

                {/* Topic tag if available */}
                {topic && (
                  <div className="flex items-center space-x-1.5">
                    <span className="text-[9px] font-semibold tracking-wider uppercase px-2 py-0.5 bg-[#F8FAFC] text-[#64748B] rounded-md border border-[#E2E8F0]">
                      Topic: {topic.name}
                    </span>
                  </div>
                )}

                {/* Action Row */}
                <div className="pt-2 border-t border-[#F1F5F9] flex items-center justify-between gap-2">
                  {/* Secondary: Open / View */}
                  <button
                    onClick={() => setActiveViewerMaterial(item)}
                    className="flex-1 py-2 px-2.5 rounded-xl border border-[#E2E8F0] bg-[#F8FAFC] hover:bg-[#F1F5F9] text-[#0F172A] text-[11px] font-bold flex items-center justify-center space-x-1.5 transition-colors"
                  >
                    <Eye className="w-3.5 h-3.5 text-[#64748B]" />
                    <span>Open Material</span>
                  </button>

                  {/* Primary CTA: Study with Guardian */}
                  <button
                    id={`study-guardian-${item.id}`}
                    onClick={() => onStudyWithGuardian(item)}
                    className="flex-1 py-2 px-2.5 rounded-xl bg-[#B8860B] hover:bg-[#A17608] text-white text-[11px] font-bold flex items-center justify-center space-x-1.5 transition-colors shadow-2xs"
                  >
                    <ShieldCheck className="w-3.5 h-3.5" />
                    <span>Study with Guardian</span>
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Add / Edit Material Modal */}
      {isAddModalOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl border border-[#E2E8F0] w-full max-w-md p-5 shadow-xl space-y-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between pb-1 border-b border-[#F1F5F9]">
              <h3 className="text-sm font-bold text-[#0F172A]">
                {editingMaterial ? 'Edit Study Material' : 'Import Study Material'}
              </h3>
              <button
                onClick={() => setIsAddModalOpen(false)}
                className="p-1 rounded-md text-[#64748B] hover:bg-[#F1F5F9]"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleSaveMaterial} className="space-y-3.5 text-xs">
              {/* Native File Picker Upload Box */}
              {!editingMaterial && (
                <div>
                  <label className="block font-semibold text-[#0F172A] mb-1">
                    Select Local File (PDF, Video, Notes, Image)
                  </label>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="video/*,application/pdf,image/*,text/*,.doc,.docx"
                    onChange={handleFileChange}
                    className="hidden"
                  />
                  <div
                    onClick={() => fileInputRef.current?.click()}
                    className="border-2 border-dashed border-[#CBD5E1] hover:border-[#B8860B] rounded-xl p-4 text-center cursor-pointer bg-[#F8FAFC] hover:bg-[#F1F5F9] transition-colors"
                  >
                    <Upload className="w-6 h-6 text-[#B8860B] mx-auto mb-1" />
                    <p className="font-semibold text-[#0F172A]">
                      {formName ? 'File Selected: Click to Change' : 'Tap to Browse Device Files'}
                    </p>
                    <p className="text-[10px] text-[#64748B] mt-0.5">
                      MP4 videos, PDFs, handwritten lecture notes, textbook images
                    </p>
                    {formFileSize && (
                      <p className="text-[10px] font-bold text-[#B8860B] mt-1">
                        Size: {formFileSize} • Type: {formType}
                      </p>
                    )}
                  </div>
                </div>
              )}

              {/* Material Name */}
              <div>
                <label className="block font-semibold text-[#0F172A] mb-1">Material Name *</label>
                <input
                  type="text"
                  value={formName}
                  onChange={e => setFormName(e.target.value)}
                  placeholder="e.g. Physics Chapter 01 Lecture 03.mp4"
                  className="w-full bg-white border border-[#E2E8F0] rounded-xl px-3 py-2 text-xs text-[#0F172A] focus:outline-none focus:border-[#B8860B]"
                  required
                />
              </div>

              {/* Material Type */}
              <div>
                <label className="block font-semibold text-[#0F172A] mb-1">Category Type</label>
                <div className="grid grid-cols-5 gap-1">
                  {(['VIDEO', 'PDF', 'NOTES', 'IMAGE', 'DOCUMENT'] as MaterialType[]).map(type => (
                    <button
                      key={type}
                      type="button"
                      onClick={() => setFormType(type)}
                      className={`py-1.5 px-1 rounded-lg text-[10px] font-bold border transition-colors ${
                        formType === type
                          ? 'bg-[#B8860B] text-white border-[#B8860B]'
                          : 'bg-white text-[#64748B] border-[#E2E8F0] hover:bg-[#F8FAFC]'
                      }`}
                    >
                      {type}
                    </button>
                  ))}
                </div>
              </div>

              {/* Curriculum Subject Association */}
              <div>
                <label className="block font-semibold text-[#0F172A] mb-1">Associated Subject *</label>
                <select
                  value={formSubjectId}
                  onChange={e => {
                    setFormSubjectId(e.target.value);
                    const chaps = chapters.filter(c => c.subjectId === e.target.value);
                    setFormChapterId(chaps[0]?.id || '');
                    const tops = topics.filter(t => t.chapterId === chaps[0]?.id);
                    setFormTopicId(tops[0]?.id || '');
                  }}
                  className="w-full bg-white border border-[#E2E8F0] rounded-xl px-3 py-2 text-xs text-[#0F172A] focus:outline-none focus:border-[#B8860B]"
                  required
                >
                  {subjects.map(s => (
                    <option key={s.id} value={s.id}>
                      {s.name}
                    </option>
                  ))}
                </select>
              </div>

              {/* Curriculum Chapter Association */}
              <div>
                <label className="block font-semibold text-[#0F172A] mb-1">Associated Chapter *</label>
                <select
                  value={formChapterId}
                  onChange={e => {
                    setFormChapterId(e.target.value);
                    const tops = topics.filter(t => t.chapterId === e.target.value);
                    setFormTopicId(tops[0]?.id || '');
                  }}
                  className="w-full bg-white border border-[#E2E8F0] rounded-xl px-3 py-2 text-xs text-[#0F172A] focus:outline-none focus:border-[#B8860B]"
                  required
                >
                  {availableChapters.map(c => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </div>

              {/* Optional Topic Association */}
              <div>
                <label className="block font-semibold text-[#0F172A] mb-1">Specific Topic (Optional)</label>
                <select
                  value={formTopicId}
                  onChange={e => setFormTopicId(e.target.value)}
                  className="w-full bg-white border border-[#E2E8F0] rounded-xl px-3 py-2 text-xs text-[#0F172A] focus:outline-none focus:border-[#B8860B]"
                >
                  <option value="">-- General Chapter Material --</option>
                  {availableTopics.map(t => (
                    <option key={t.id} value={t.id}>
                      {t.name}
                    </option>
                  ))}
                </select>
              </div>

              {/* Notes / Summary */}
              <div>
                <label className="block font-semibold text-[#0F172A] mb-1">Study Notes / Synopsis</label>
                <textarea
                  value={formNotes}
                  onChange={e => setFormNotes(e.target.value)}
                  placeholder="Key formulas, important derivation remarks, lecture references..."
                  rows={2}
                  className="w-full bg-white border border-[#E2E8F0] rounded-xl p-2.5 text-xs text-[#0F172A] focus:outline-none focus:border-[#B8860B] resize-none"
                />
              </div>

              {fileError && (
                <p className="text-red-600 text-[11px] font-medium flex items-center space-x-1">
                  <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                  <span>{fileError}</span>
                </p>
              )}

              <div className="pt-2 flex items-center justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => setIsAddModalOpen(false)}
                  className="px-4 py-2 border border-[#E2E8F0] text-[#64748B] hover:bg-[#F8FAFC] rounded-xl text-xs font-bold transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 bg-[#B8860B] hover:bg-[#A17608] text-white rounded-xl text-xs font-bold transition-colors shadow-xs"
                >
                  {editingMaterial ? 'Update Resource' : 'Save to Library'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* In-App Delete Confirmation Modal */}
      {materialToDelete && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl border border-[#E2E8F0] w-full max-w-sm p-5 shadow-2xl space-y-4 animate-in fade-in">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-2.5">
                <div className="w-8 h-8 rounded-xl bg-red-50 border border-red-100 flex items-center justify-center text-red-600 shrink-0">
                  <Trash2 className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-[#0F172A]">Delete Resource?</h3>
                  <p className="text-[10px] text-[#64748B]">Permanent library deletion</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setMaterialToDelete(null)}
                className="p-1.5 rounded-lg text-[#64748B] hover:bg-[#F1F5F9] transition-colors"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <p className="text-xs text-[#64748B] leading-relaxed">
              Are you sure you want to delete <strong className="text-[#0F172A]">"{materialToDelete.name}"</strong>? This will permanently remove it from your device library and data store.
            </p>

            <div className="flex items-center justify-end space-x-2 pt-2 border-t border-[#F1F5F9]">
              <button
                type="button"
                onClick={() => setMaterialToDelete(null)}
                className="px-3.5 py-1.5 rounded-xl text-xs font-semibold text-[#64748B] hover:bg-[#F1F5F9] border border-[#E2E8F0] transition-colors"
              >
                Cancel
              </button>
              <button
                type="button"
                id="confirm-delete-material-btn"
                onClick={() => {
                  const idToDelete = materialToDelete.id;
                  setMaterialToDelete(null);
                  if (activeViewerMaterial?.id === idToDelete) {
                    setActiveViewerMaterial(null);
                  }
                  onDeleteMaterial(idToDelete);
                }}
                className="px-4 py-1.5 rounded-xl text-xs font-bold text-white bg-red-600 hover:bg-red-700 shadow-xs transition-colors flex items-center space-x-1.5"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>Delete Resource</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Full Material Viewer Modal */}
      {activeViewerMaterial && (() => {
        const viewerBadge = getTypeBadge(activeViewerMaterial.type);
        const ViewerIcon = viewerBadge.icon;
        const viewerSubject = subjects.find(s => s.id === activeViewerMaterial.subjectId);
        const viewerChapter = chapters.find(c => c.id === activeViewerMaterial.chapterId);

        return (
          <div className="fixed inset-0 bg-black/70 flex items-center justify-center p-4 z-50">
            <div className="bg-white rounded-2xl border border-[#E2E8F0] w-full max-w-lg p-5 shadow-2xl space-y-3 max-h-[92vh] flex flex-col">
              {/* Modal Header */}
              <div className="flex items-center justify-between pb-2 border-b border-[#F1F5F9] shrink-0">
                <div className="flex items-center space-x-2 min-w-0 pr-2">
                  <div className="w-7 h-7 rounded-lg bg-[#FEFCE8] border border-[#FEF08A] flex items-center justify-center text-[#B8860B] shrink-0">
                    <ViewerIcon className="w-4 h-4" />
                  </div>
                  <div className="min-w-0">
                    <h3 className="text-xs font-bold text-[#0F172A] truncate">
                      {activeViewerMaterial.name}
                    </h3>
                    <p className="text-[10px] text-[#64748B]">
                      {viewerSubject?.name || 'General'} •{' '}
                      {viewerChapter?.name || 'Curriculum'}
                    </p>
                  </div>
                </div>
                <button
                  onClick={() => setActiveViewerMaterial(null)}
                  className="p-1.5 rounded-lg text-[#64748B] hover:bg-[#F1F5F9]"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              {/* Media Content Display */}
              <div className="flex-1 overflow-y-auto min-h-[220px] rounded-xl bg-[#F8FAFC] border border-[#E2E8F0] p-3 flex flex-col items-center justify-center">
                {activeViewerMaterial.type === 'VIDEO' ? (
                  <div className="w-full space-y-2">
                    <video
                      controls
                      autoPlay
                      playsInline
                      src={activeViewerMaterial.uriOrPath}
                      className="w-full max-h-[300px] rounded-lg bg-black object-contain shadow-xs"
                    />
                    <p className="text-[10px] text-center text-[#64748B]">
                      Native media playback active with full seek and speed controls
                    </p>
                  </div>
                ) : activeViewerMaterial.type === 'IMAGE' ? (
                  <div className="w-full space-y-2 text-center">
                    <img
                      src={activeViewerMaterial.uriOrPath}
                      alt={activeViewerMaterial.name}
                      className="max-h-[320px] mx-auto rounded-lg object-contain shadow-xs border border-[#E2E8F0]"
                    />
                    <p className="text-[10px] text-[#64748B]">High-resolution academic diagram / notes sheet</p>
                  </div>
                ) : activeViewerMaterial.type === 'PDF' ? (
                  <div className="w-full space-y-3 p-3 text-center">
                    <div className="w-12 h-12 rounded-2xl bg-red-50 border border-red-100 flex items-center justify-center text-red-600 mx-auto">
                      <FileText className="w-6 h-6" />
                    </div>
                    <div>
                      <h4 className="text-xs font-bold text-[#0F172A]">{activeViewerMaterial.name}</h4>
                      <p className="text-[11px] text-[#64748B] mt-1">
                        Portable Document Format (PDF) • Ready for Study with Guardian
                      </p>
                    </div>
                    {activeViewerMaterial.notes && (
                      <div className="bg-white p-3 rounded-xl border border-[#E2E8F0] text-left text-xs text-[#0F172A] max-h-36 overflow-y-auto">
                        <p className="font-bold text-[10px] text-[#B8860B] uppercase tracking-wider mb-1">
                          Lecture Outline & Reference Notes:
                        </p>
                        <p className="whitespace-pre-wrap leading-relaxed">{activeViewerMaterial.notes}</p>
                      </div>
                    )}
                  </div>
                ) : (
                  <div className="w-full space-y-2 text-left p-2">
                    <div className="bg-white p-4 rounded-xl border border-[#E2E8F0] text-xs text-[#0F172A] font-mono whitespace-pre-wrap leading-relaxed max-h-[300px] overflow-y-auto">
                      {activeViewerMaterial.uriOrPath.startsWith('data:')
                        ? 'Encoded study document resource'
                        : activeViewerMaterial.uriOrPath || activeViewerMaterial.notes || 'No text content attached.'}
                    </div>
                    <p className="text-[10px] text-[#64748B] text-center">Handwritten note transcription / text summary</p>
                  </div>
                )}
              </div>

              {/* Bottom Actions inside Viewer */}
              <div className="pt-2 border-t border-[#F1F5F9] flex items-center justify-between gap-2 shrink-0">
                <button
                  onClick={() => {
                    const item = activeViewerMaterial;
                    setActiveViewerMaterial(null);
                    onStudyWithGuardian(item);
                  }}
                  className="w-full py-2.5 px-4 rounded-xl bg-[#B8860B] hover:bg-[#A17608] text-white text-xs font-bold flex items-center justify-center space-x-2 transition-colors shadow-xs"
                >
                  <ShieldCheck className="w-4 h-4" />
                  <span>Study with Guardian</span>
                </button>
              </div>
            </div>
          </div>
        );
      })()}
    </div>
  );
};
